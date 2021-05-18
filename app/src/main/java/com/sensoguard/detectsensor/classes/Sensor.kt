package com.sensoguard.detectsensor.classes


class Sensor {

    private var longitude: Double?=null
    private var latitude: Double?=null
    private var name: String?=null
    private var id: String? = null
    private var type: String? = "Seismic"
    private var isArmed = false
    var isLocallyDefined:Boolean=false
    //private var type:Int?=null

    constructor( _uid: String?,_name: String?){
        id=_uid
        name = _name
        isArmed = true
    }
    constructor( _id: String?){
        id=_id
        name= "id-$id"
        isArmed = true
    }

    fun getId():String{
        return id.toString()
    }

    fun setId(_id:String){
         id=_id
    }

    fun getName(): String? {
        return name
    }

    fun setName(_name: String) {
        name = _name
    }

    fun getType(): String? {
        return type
    }

    fun setType(_type: String?) {
        type = _type
    }

    fun setArm(state: Boolean) {
        isArmed = state
    }

    fun isArmed(): Boolean {
        return isArmed
    }

    fun getLatitude(): Double? {
        return latitude
    }

    fun getLongtitude(): Double? {
        return longitude
    }

    fun setLatitude(_latitude:Double?){
        latitude=_latitude
    }
    fun setLongtitude(_longitude:Double?){
        longitude=_longitude
    }
}