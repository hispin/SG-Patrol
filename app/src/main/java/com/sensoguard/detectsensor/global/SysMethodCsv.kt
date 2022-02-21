package com.sensoguard.detectsensor.global

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.core.content.FileProvider
import com.opencsv.CSVWriter
import com.sensoguard.detectsensor.R
import com.sensoguard.detectsensor.classes.Alarm
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*


const val mySeparator='@'
const val mfolderName="sansorgaurd"
const val mfileName="SG-AlarmLog.csv"


//var mCsvAlarms:ArrayList<String>?=null

private fun alarmTitlesToCsvString(context: Context): String {

    var res = context.resources
    val placeHolderBitmap = "Bitmap"

    //val mySeparator=";"
    val sb = StringBuilder()
    sb.append(res.getString(R.string.csv_date_title).toString())
    sb.append(mySeparator)
    sb.append(res.getString(R.string.csv_name_title).toString())
    sb.append(mySeparator)
    sb.append(res.getString(R.string.csv_id_title).toString())
    sb.append(mySeparator)
    sb.append(res.getString(R.string.csv_alarm_type_title).toString())
    sb.append(mySeparator)
    sb.append(res.getString(R.string.csv_latitude_title).toString())
    sb.append(mySeparator)
    sb.append(res.getString(R.string.csv_longitude_title).toString())


    return sb.toString()
}

private fun alarmToCsvString(alarm: Alarm?, context: Context): String {


    val placeHolderBitmap="Bitmap"

    //val mySeparator=";"
    val sb=StringBuilder()
    sb.append(alarm?.currentDate)
    sb.append(mySeparator)
    sb.append(alarm?.name)
    sb.append(mySeparator)
    sb.append(alarm?.id)
    sb.append(mySeparator)
    sb.append(alarm?.type)
    sb.append(mySeparator)
    sb.append(alarm?.latitude)
    sb.append(mySeparator)
    sb.append(alarm?.longitude)


    return sb.toString()
}

fun writeCsvFile(mCsvAlarms: ArrayList<String>,context:Context): Boolean {
    try {
        //create a folder

        val folder = File(context.externalCacheDir, mfolderName)
        if (!folder.exists()) {
            folder.mkdirs()
        }

        if(context.externalCacheDir==null){
            return false
        }

        //write to file
        val baseDir = context.externalCacheDir!!.absolutePath
        val filePath = baseDir + "/" + mfolderName + "" + File.separator + mfileName
        //val filePath=baseDir + File.separator + mfileName

        //val os: OutputStream = FileOutputStream(filePath)
        //val w = PrintWriter(OutputStreamWriter(os, "UTF-8"))
//         val iteratorList = mCsvAlarms.listIterator()
//
//        while (iteratorList != null && iteratorList.hasNext()) {
//            val item=iteratorList.next()
//            val strArr=item.split(mySeparator)
//            w.print(strArr.toTypedArray())
//        }
//
//         w.flush()
//         w.close()


        //////////////////

//         StreamResource(object : StreamSource() {
//             val stream: InputStream?
//                 get() {
//                     val exportData: List<Array<Any>> = getExportData()
//                     val buffer = StringBuffer()
//                     buffer.append(someData)
//                     val bytes = buffer.toString().toByteArray()
//                     return ByteArrayInputStream(bytes)
//                 }
//         }, "MyExport.csv")

        //////////////////
        val file = File(filePath)
        val fileWriter = FileWriter(file)

        val writer = CSVWriter(fileWriter)

        val iteratorList = mCsvAlarms.listIterator()

        while (iteratorList != null && iteratorList.hasNext()) {
            val item = iteratorList.next()

            val strArr = item.split(mySeparator)
            writer.writeNext(strArr.toTypedArray())

        }
        writer.close()
        fileWriter.close()
        /////////////////////


        //val photoFiles=true
        //todo-Haggay: The CsvFile has all data needed to write the file - COMPLETE
        //create the file in the folder given & filename given
        //loop through csvFile.getmCsvLines write each String item as a separate line
        //todo: Make sure run is not ic_on the Main Thread, use runnable Thread or Intent if needed: Answer: intent service is already a diff thread

        //return false if failed (set 'photoFiles' to false in IOException)
        //return photoFiles //Change this as needed

        return true
    } catch (e: Exception) {
        //Log.d("testcsv",e.cause.toString())
        //Log.d("testcsv",e.message.toString())
        e.printStackTrace()
    }
    return false
}


//Share the csv file
fun shareCsv(activity: Activity) {

    val baseDir = activity.externalCacheDir?.absolutePath
    val filePath = baseDir + "/" + mfolderName + "" + File.separator + mfileName
    val file = File(filePath)
    val sendIntent = Intent(Intent.ACTION_SEND)
    // sendIntent.setType("text/html");
    sendIntent.type = "application/csv"
    sendIntent.putExtra(Intent.EXTRA_EMAIL, "")

    val resources = activity.resources
    val locale =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) resources.configuration.locales.getFirstMatch(
            resources.assets.locales
        )
        else resources.configuration.locale
    val dateFormat = SimpleDateFormat("dd/MM/yy", locale)
    val dateString = dateFormat.format(Calendar.getInstance().time)

    sendIntent.putExtra(Intent.EXTRA_SUBJECT, "SensoGuard Alarm Log $dateString")
    sendIntent.putExtra(Intent.EXTRA_TEXT, "")
    //val uri = Uri.fromFile(file)
    val uri = FileProvider.getUriForFile(
        activity,
        "com.example.homefolder.example.provider", //(use your app signature + ".provider" )
        file
    )
    sendIntent.putExtra(Intent.EXTRA_STREAM, uri)
    activity.startActivity(Intent.createChooser(sendIntent, "Share file"))
}

fun alarmsListToCsvFile(alarms: ArrayList<Alarm>?, context: Context): ArrayList<String> {


    var mCsvAlarms=ArrayList<String>()

    val iteratorList=alarms?.listIterator()

    val csvString= alarmTitlesToCsvString(context)
    SyncProcesses.getInstance().syncAddItemToList(mCsvAlarms, csvString)

    while (iteratorList != null && iteratorList.hasNext()) {
        val item=iteratorList.next()
        val csvString= alarmToCsvString(item, context)
        SyncProcesses.getInstance().syncAddItemToList(mCsvAlarms, csvString)
    }
   return mCsvAlarms

//        var csvLines: java.util.ArrayList<String>?=null
//        var result: CsvFile?=null
//        result?.setFolderName(folderName)
//        result?.setFileName(fileName)
    //todo-Haggay:
    //(a) iterate through each PhotoFile object item in 'alarms'
    //(b) Convert each PhotoFile object to a csvString using function photoFileToCsvString(..)
    //(c) Add each csvString as a String item 'csvLines' ArrayList<String>
    // now the photoFiles String can be assigned to

    //photoFiles?.setCsvLines(todo: here insert the concatenated string of all the photo files)
    //return result
}


//fun alarmListToCsvFile(alarms :ArrayList<Alarm>?){
//
//    mCsvAlarms=ArrayList()
//    val collect=
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
//            alarms?.joinToString(mySeparator.toString())
//        } else {
//            //TODO("VERSION.SDK_INT < N") -COMPLETE
//            val sb=StringBuilder()
//            val iteratorList=alarms?.listIterator()
//
//            while (iteratorList != null && iteratorList.hasNext()) {
//                val item=iteratorList.next()
//                sb.append(item.toString())
//                sb.append(mySeparator)
//            }
//            // last string, no mySeparator
//            if(alarms?.size!=null
//                && alarms.size > 0){
//                sb.append(alarms[alarms.size -1])
//            }
//            sb.toString()
//        }
//
//    collect?.let { mCsvAlarms?.add(it) }
//    //todo-Haggay:-COMPLETE
//    //(a) Iterate through each Uri item in the input arg 'alarms'.
//    //(b) Convert each Uri to String I advise to uri.toString() & add the String to the csvLines ArrayList
//    //(c) When done loop use the set methods to insert into the photoFiles CsvFile
//}
