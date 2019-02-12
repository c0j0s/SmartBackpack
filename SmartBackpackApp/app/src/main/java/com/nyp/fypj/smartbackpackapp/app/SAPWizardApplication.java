package com.nyp.fypj.smartbackpackapp.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.webkit.CookieManager;
import com.nyp.fypj.smartbackpackapp.R;
import com.nyp.fypj.smartbackpackapp.service.SAPServiceManager;
import com.nyp.fypj.smartbackpackapp.logon.ClientPolicyManager;
import com.nyp.fypj.smartbackpackapp.logon.LogonActivity;
import com.nyp.fypj.smartbackpackapp.logon.SecureStoreManager;


import com.sap.cloud.mobile.foundation.configurationprovider.ConfigurationLoader;
import com.sap.cloud.mobile.foundation.common.ClientProvider;
import com.sap.cloud.mobile.foundation.common.SettingsParameters;
import com.sap.cloud.mobile.foundation.authentication.AppLifecycleCallbackHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.cloud.mobile.foundation.networking.WebkitCookieJar;
import com.sap.cloud.mobile.foundation.authentication.BasicAuthDialogAuthenticator;
import com.sap.cloud.mobile.foundation.networking.AppHeadersInterceptor;
import com.nyp.fypj.smartbackpackapp.logon.BasicAuthPersistentCredentialStore;
import java.util.concurrent.TimeUnit;

import java.net.MalformedURLException;

import okhttp3.OkHttpClient;

/**
 * This class extends the {@link Application} class. Its purpose is to configure application-wide services such as
 * error handling and data access and provide access to them. It maintains an
 * {@link ActivityLifecycleCallbacks} instance, as well. By extending the callback's default
 * implementation the application will be able to react on lifecycle events of the contained activities.
 */
public class SAPWizardApplication extends Application {

    private static final Logger LOGGER = LoggerFactory.getLogger(SAPWizardApplication.class);

    /**
     * ID of the Mobile Services endpoint configured for this application.
     */
    public static final String APPLICATION_ID = "com.nyp.fypj.smartbackpackapp";

    /**
     * Application version sent to Mobile Services, which may be used to control access from outdated
     * clients.
     */
    public static final String APPLICATION_VERSION = "1.0";

    /**
     * Manages and provides access to OData stores providing data for the app.
     */
    private SAPServiceManager sapServiceManager;

    /**
     * Manages and provides access to secure key-value-stores used to persist settings and user data.
     */
    private SecureStoreManager secureStoreManager;

    /**
     * Manages and provides access to local and server-provided client policies, including but not
     * limited to passcode requirements, retry count during unlocking etc.
     */
    private ClientPolicyManager clientPolicyManager;

    /**
     * Global error handler displaying error messages to the user.
     */
    private ErrorHandler errorHandler;
	
	/**
     * Lifecycle observer, listens for foreground-background state changes.
     */
    private SAPWizardLifecycleObserver sapWizardLifecycleObserver;

    /**
     * Persistent credential store for {@link BasicAuthPersistentCredentialStore},
     * which authenticates HTTP sessions.
     */
    private BasicAuthPersistentCredentialStore basicAuthPersistentCredentialStore;

    /**
     * Utility class implementing CRUD access to OData services on top of the underlying OData
     * stores.
     */
    /**
     * Provides access to locally persisted configuration that is loaded via {@link ConfigurationLoader}.
     */
    private ConfigurationData configurationData;

    /**
     * Returns the application-wide service manager.
     *
     * @return the service manager
     */
    public SAPServiceManager getSAPServiceManager() {
        return sapServiceManager;
    }

    /**
     * Returns the application-wide secure store manager.
     *
     * @return the secure store manager
     */
    public SecureStoreManager getSecureStoreManager() { return secureStoreManager; }

    /**
     * Returns the application-wide client policy manager.
     *
     * @return the client policy manager
     */
    public ClientPolicyManager getClientPolicyManager() { return clientPolicyManager; }

    /**
     * Returns the application-wide error handler.
     *
     * @return the error handler
     */
    public ErrorHandler getErrorHandler() {
         return errorHandler;
    }

    /**
     * Returns the application-wide credential store.
     *
     * @return the credential store.
     */
    public BasicAuthPersistentCredentialStore getBasicAuthPersistentCredentialStore() { return basicAuthPersistentCredentialStore; }

    /**
     * Returns the application-wide data content utilities.
     *
     * @return the data content utilities
     */

    /**
     * Returns the application-wide configuration data.
     *
     * @return the configuration data
     */
    public ConfigurationData getConfigurationData() {
        return configurationData;
    }

    /**
     * Asks confirmation from the user if the application data should be reset, and resets the app
     * if the user confirms the prompt.
     */
    public void resetAppWithUserConfirmation() {
        Activity activity = AppLifecycleCallbackHandler.getInstance().getActivity();
        AlertDialog.Builder alert = new AlertDialog.Builder(activity, R.style.AlertDialogStyle)
                .setMessage(R.string.reset_app_confirmation)
                // Setting OK Button
                .setPositiveButton(R.string.yes, (dialog, which) -> {
                   // reset the application
                   resetApp(activity);
                   LOGGER.info("Yes button is clicked. The all information related to this application will be deleted.");
                 })
                 // Setting Cancel Button
                .setNegativeButton(R.string.cancel, (dialog, which) -> {
                   LOGGER.info("The Cancel button is clicked.");
                });
         alert.show();
     }

    /**
     * Clears all volatile and persisted configuration that was loaded via {@link ConfigurationLoader}.
     */
    private void resetConfigurationData() {
        ConfigurationData.resetPersistedConfiguration(getApplicationContext());
        configurationData.resetData();
    }
	

    /**
     * Clears all user-specific data and configuration from the application, essentially resetting
     * it to its initial state.
     *
     * @param activity Activity from which the request originates
     */
    public void resetApp(Activity activity) {
        secureStoreManager.resetStores();
        resetConfigurationData();
        clearCookies(activity);
        resetOnboardingState();
        restartApp(activity);
    }

    /**
     * Clears all cookies, making sure no sessions remain in the HTTP client.
     *
     * @param activity Activity from which the request originates
     */
    private void clearCookies(Activity activity) {
        CookieManager webkitCookieManager = CookieManager.getInstance();

        activity.runOnUiThread(() -> webkitCookieManager.removeAllCookies(success -> {
            if (success) {
                LOGGER.info("Cookies are deleted.");
            } else {
                LOGGER.error("Cookies couldn't be removed!");
            }
        }));
    }

    /**
     * Resets the persisted onboarding state of the application to the initial state.
     */
    private void resetOnboardingState() {
        setIsOnboarded(false);
    }

    /**
     * Restarts the app by presenting the logon screen.
     *
     * @param activity Activity from which the request originates
     */
    private void restartApp(Activity activity) {
        Intent intent = new Intent(activity, LogonActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        activity.startActivity(intent);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        startErrorHandler();

        secureStoreManager = new SecureStoreManager(this);
        configurationData = new ConfigurationData(this, errorHandler);
        basicAuthPersistentCredentialStore = new BasicAuthPersistentCredentialStore(secureStoreManager);
        sapServiceManager = new SAPServiceManager(configurationData);
        clientPolicyManager = new ClientPolicyManager(this);
		sapWizardLifecycleObserver = new SAPWizardLifecycleObserver(secureStoreManager);
		
        registerLifecycleCallbacks();
        if (isOnboarded()) {
            configurationData.loadData();
        }
        initHttpClient();
    }

    /**
     * Creates a global error handler shared by all app components and starts its background thread.
     */
    private void startErrorHandler() {
         errorHandler = new ErrorHandler("SAPWizardErrorHandler");
         ErrorPresenter presenter = new ErrorPresenterByNotification(this);
         errorHandler.setPresenter(presenter);
         errorHandler.start();
    }

    /**
     * Registers the SDK-provided lifecycle callback listener for this application.
     */
    private void registerLifecycleCallbacks() {
         registerActivityLifecycleCallbacks(AppLifecycleCallbackHandler.getInstance());
    }
	

    /**
     * Configures the shared HTTP client that is used by both the application and the SAP SDK.
     */
    private void initHttpClient() {
        String deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                 .addInterceptor(new AppHeadersInterceptor(APPLICATION_ID, deviceId, APPLICATION_VERSION))
                 .authenticator(new BasicAuthDialogAuthenticator(basicAuthPersistentCredentialStore))
                 .cookieJar(new WebkitCookieJar())
                 .connectTimeout(30, TimeUnit.SECONDS)
                 .build();
        ClientProvider.set(okHttpClient);
    }

   /**
    * Creates an application parameter object, including the application ID, version and Mobile Services
    * URL.
    *
    * @return Application metadata used for Mobile Services
    */
    public SettingsParameters getSettingsParameters() {
        try {
            String serviceUrl = configurationData.getServiceUrl();
            String deviceId = Settings.Secure.getString(this.getContentResolver(), Settings.Secure.ANDROID_ID);
            return new SettingsParameters(serviceUrl, APPLICATION_ID, deviceId, APPLICATION_VERSION);
        } catch (MalformedURLException e) {
            String title = getResources().getString(R.string.configuration_invalid);
            String details = String.format(getResources().getString(R.string.configuration_contained_malformed_url), e.getMessage());
            ErrorMessage errorMessage = new ErrorMessage(title, details, e, false);
            getErrorHandler().sendErrorMessage(errorMessage);
        }
        return null;
    }

    /**
     * Tells if this application has finished user onboarding.
     *
     * @return if this application is onboarded
     */
    public boolean isOnboarded() {
        return secureStoreManager.isOnboarded();
    }

    /**
     * Sets the onboarding state of this application.
     *
     * @param isOnboarded new onboarding state
     */
    public void setIsOnboarded(boolean isOnboarded) {
        secureStoreManager.setIsOnboarded(isOnboarded);
    }

}
