import tensorflow as tf

converter = tf.lite.TFLiteConverter.from_saved_model("/tmp/saved_model.pb")
tflite_model = converter.convert()
open("./model/sbp_model.tflite", "wb").write(tflite_model)
