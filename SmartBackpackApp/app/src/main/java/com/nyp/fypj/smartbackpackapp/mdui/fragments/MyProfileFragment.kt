package com.nyp.fypj.smartbackpackapp.mdui.fragments

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.nyp.fypj.smartbackpackapp.R
import com.nyp.fypj.smartbackpackapp.service.IotDataMLServiceManager
import com.sap.cloud.android.odata.sbp.IotdeviceinfoType
import com.sap.cloud.android.odata.sbp.UserDevicesType
import com.sap.cloud.android.odata.sbp.UserinfosType
import kotlinx.android.synthetic.main.fragment_my_profile.view.*
import kotlinx.android.synthetic.main.notification_template_lines_media.view.*
import android.app.AlarmManager
import android.support.v4.content.ContextCompat.getSystemService
import android.app.PendingIntent
import android.content.Intent
import com.nyp.fypj.smartbackpackapp.mdui.MainActivity
import android.support.v4.app.ActivityCompat.finishAffinity




private const val USER_PROFILE = "userProfile"
private const val USER_DEVICES = "userDevices"

class MyProfileFragment : Fragment() {

    private lateinit var userProfile: UserinfosType
    private lateinit var userDevices: ArrayList<IotdeviceinfoType>
    private var listener: OnFragmentInteractionListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            try {
                userProfile = it.getParcelable(USER_PROFILE)!!
                userDevices = it.getParcelableArrayList(USER_DEVICES)!!
            }catch (e:Exception){
                val intent = Intent(activity, MainActivity::class.java)
                this.startActivity(intent)
            }
        }

    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val rootView = inflater.inflate(R.layout.fragment_my_profile, container, false)
        activity!!.title = "My Profile"
        rootView.ph_profile_ovp.headline = userProfile.name
        rootView.ph_profile_ovp.subheadline = userProfile.userId
        rootView.ph_profile_ovp.description = if(userProfile.gender == "M") "Male" else "Female" + ", ${userProfile.race}"

        rootView.spf_asthmatic_level.value = userProfile.asthmaticDesc
        rootView.spf_dob_age.value = "${userProfile.dob.date}, ${userProfile.age}"
        rootView.spf_user_contact.value = userProfile.contactNo
        rootView.spf_user_address.value = "${userProfile.userCity}, ${userProfile.userState}, ${userProfile.userCountry}".toLowerCase()
        return rootView
    }

    // TODO: Rename method, update argument and hook method into UI event
    fun onButtonPressed(uri: Uri) {
        listener?.onFragmentInteraction(uri)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
//        if (context is OnFragmentInteractionListener) {
//            listener = context
//        } else {
//            throw RuntimeException(context.toString() + " must implement OnFragmentInteractionListener")
//        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     *
     *
     * See the Android Training lesson [Communicating with Other Fragments]
     * (http://developer.android.com/training/basics/fragments/communicating.html)
     * for more information.
     */
    interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        fun onFragmentInteraction(uri: Uri)
    }

    companion object {

        private const val TAG = "MyProfileFragment"

        @JvmStatic
        fun newInstance(param1: UserinfosType, param2: ArrayList<UserDevicesType>) =
                HomeFragment().apply {
                    arguments = Bundle().apply {
                        putParcelable(USER_PROFILE, param1)
                        putParcelableArrayList(USER_DEVICES, param2)
                    }
                }
    }
}
