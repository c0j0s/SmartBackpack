package com.nyp.fypj.smartbackpackapp.bluetooth

class HoldingZoneData (private var recorededOn:String, private var raw: String) {

    var humidity:String = ""
    var temperature:String = ""
    var pm2_5:String = ""
    var pm10:String = ""
    var predictedComfortLevel:String = ""
    var alertTriggered:String = ""

    init {
        var arr = raw.split(";")
        humidity = arr[0]
        temperature = arr[1]
        pm2_5 = arr[2]
        pm10 = arr[3]
        predictedComfortLevel = arr[4]
        alertTriggered = arr[5]
    }
}