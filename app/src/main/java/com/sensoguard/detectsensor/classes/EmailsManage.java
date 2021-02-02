//package com.sensoguard.detectsensor.classes;
//
//import android.app.NotificationChannel;
//import android.app.NotificationManager;
//import android.app.PendingIntent;
//import android.content.Context;
//import android.content.Intent;
//import android.os.Build;
//import android.os.Environment;
//import android.os.SystemClock;
//import android.util.Log;
//
//import androidx.core.app.NotificationCompat;
//
//import com.sensoguard.hunter.R;
//import com.sensoguard.hunter.activities.InitAppActivity;
//
//import org.apache.commons.lang3.ArrayUtils;
//import org.apache.commons.lang3.StringUtils;
//
//import java.io.File;
//import java.io.FileNotFoundException;
//import java.io.FileOutputStream;
//import java.io.IOException;
//import java.io.InputStream;
//import java.lang.ref.WeakReference;
//import java.util.ArrayList;
//import java.util.Calendar;
//import java.util.Date;
//import java.util.List;
//import java.util.ListIterator;
//import java.util.Objects;
//import java.util.Properties;
//
//import javax.mail.Address;
//import javax.mail.BodyPart;
//import javax.mail.Folder;
//import javax.mail.Message;
//import javax.mail.MessagingException;
//import javax.mail.Multipart;
//import javax.mail.NoSuchProviderException;
//import javax.mail.Part;
//import javax.mail.Session;
//import javax.mail.Store;
//import javax.mail.internet.InternetAddress;
//import javax.mail.internet.MimeMultipart;
//import javax.mail.search.ComparisonTerm;
//import javax.mail.search.ReceivedDateTerm;
//
//import static com.sensoguard.hunter.global.ConstsKt.ADD_ATTACHED_PHOTOS_KEY;
//import static com.sensoguard.hunter.global.ConstsKt.ALARM_OTHER;
//import static com.sensoguard.hunter.global.ConstsKt.CHANNEL_ID;
//import static com.sensoguard.hunter.global.ConstsKt.CHANNEL_NAME;
//import static com.sensoguard.hunter.global.ConstsKt.CREATE_ALARM_ID_KEY;
//import static com.sensoguard.hunter.global.ConstsKt.CREATE_ALARM_KEY;
//import static com.sensoguard.hunter.global.ConstsKt.CREATE_ALARM_NAME_KEY;
//import static com.sensoguard.hunter.global.ConstsKt.CREATE_ALARM_TYPE_KEY;
//import static com.sensoguard.hunter.global.ConstsKt.DETECT_ALARM_KEY;
//import static com.sensoguard.hunter.global.ConstsKt.ERROR_RESULT_VALIDATION_EMAIL_ACTION;
//import static com.sensoguard.hunter.global.ConstsKt.ERROR_VALIDATION_EMAIL_MSG_KEY;
//import static com.sensoguard.hunter.global.ConstsKt.IS_MYSCREENACTIVITY_FOREGROUND;
//import static com.sensoguard.hunter.global.ConstsKt.LAST_DATE_ALARM;
//import static com.sensoguard.hunter.global.SysMethodsSharedPrefKt.getBooleanInPreference;
//import static com.sensoguard.hunter.global.SysMethodsSharedPrefKt.getLongInPreference;
//import static com.sensoguard.hunter.global.SysMethodsSharedPrefKt.setLongInPreference;
//import static com.sensoguard.hunter.global.SysMethodsStorageKt.getAlarmsFromLocally;
//import static com.sensoguard.hunter.global.SysMethodsStorageKt.getSensorsFromLocally;
//import static com.sensoguard.hunter.global.SysMethodsStorageKt.writeFile;
//
//
//public class EmailsManage {
//    private static final EmailsManage ourInstance = new EmailsManage();
//    //private static Alarm prevAlarm = new Alarm();
//    private static Calendar lastEmailDate = null;
//
//    private EmailsManage() {
//    }
//
//    public static EmailsManage getInstance() {
//        return ourInstance;
//    }
//
//
//    //read camera's unread emails of last day
//    public void readeLastDayUnreadEmails(MyEmailAccount myEmailAccount, Context context) throws IOException {
//
//        Properties props = new Properties();
//        String protocol = null;
//
//        //IMAPS protocol
//        if (myEmailAccount != null && myEmailAccount.getEmailServer() != null) {
//            protocol = myEmailAccount.getEmailServer().substring(0, myEmailAccount.getEmailServer().indexOf("."));
//            props.setProperty("mail.store.protocol", protocol);
//        } else {
//            return;
//        }
//
//        //Setting IMAP session
//        Session imapSession = getImapSession(myEmailAccount, props);
//        if (imapSession == null) {
//            return;
//        }
//
//
//        Store store = null;
//        try {
//            store = imapSession.getStore(protocol);
//        } catch (NoSuchProviderException e) {
//            //writeFile("readeLastDayUnreadEmails", context);
//            e.printStackTrace();
//        }
//
//        //Connect to server by sending username and password.
//        store = connectToServer(store, myEmailAccount, context);
//
//        if (store == null) {
//            return;
//        }
//
//
//        //Get all mails in Inbox Folder
//        Folder inbox = getInbox(store, context);
//
//
//        //Return result to array of message
//        try {
//            if (inbox != null) {
//
//                long longLastDateAlarm = getLongInPreference(context, LAST_DATE_ALARM, -1);
//                if (longLastDateAlarm == -1) {
//                    lastEmailDate = Calendar.getInstance();
//                    lastEmailDate.roll(Calendar.DATE, false);
//                } else {
//                    lastEmailDate = Calendar.getInstance();
//                    lastEmailDate.setTimeInMillis(longLastDateAlarm);
//                }
//
//                //String lastDate = getStringFromCalendar(lastEmailDate, "kk:mm dd/MM/yy", context);
//                //Log.d("testEmails", "lastDate" + lastDate);
//
//                Calendar currentDate = Calendar.getInstance();
//                currentDate.roll(Calendar.DATE, false);
//                //String da = getStringFromCalendar(currentDate, "kk:mm dd/MM/yy", context);
//                //Log.d("testEmails", "currentDate" + da);
//
//
//                Message[] unReadLastDayMsgs = inbox.search(new ReceivedDateTerm(ComparisonTerm.GT, currentDate.getTime()));
//
//                if (unReadLastDayMsgs.length < 1) {
//                    return;
//                }
//                //reverse : the newer message is the first
//                ArrayUtils.reverse(unReadLastDayMsgs);
//                for (Message unReadLastDayMsg : unReadLastDayMsgs) {
//
//
//                    //if received date of this email is already scanned
//                    Date receivedDate = unReadLastDayMsg.getReceivedDate();
//                    if (receivedDate != null
//                            && (lastEmailDate.getTime().after(receivedDate) || lastEmailDate.getTime().compareTo(receivedDate) == 0)) {
//                        lastEmailDate.setTime(unReadLastDayMsgs[0].getReceivedDate());
//                        setLongInPreference(context, LAST_DATE_ALARM, lastEmailDate.getTimeInMillis());
//
//                        //close connection
//                        closeConnection(store, context);
//                        return;
//                    }
//                    //Log.d("testEmails", unReadLastDayMsg.getSubject());
//
//
//                    //check if the email is already exist
//                    if (isEmailAlreadyReceived(unReadLastDayMsg.getMessageNumber(), context)) {
//                        //go to next email
//                        continue;
//                    }
//
//                    String[] arrMySubject = new String[1];
//                    //get the camera with the same email from and the same model if exist
//                    Camera camera = getAppropriateCamera(unReadLastDayMsg, context, arrMySubject);
//                    if (camera == null || arrMySubject[0].equals("-1")) {
//                        //go to next email
//                        continue;
//                    }
//
//                    String mySubject = arrMySubject[0];
//
//
//                    //change the message to read email
//                    //inbox.setFlags(new Message[]{unReadLastDayMsg}, new Flags(Flags.Flag.SEEN), true);
//
//
//                    Alarm myAlarm = new Alarm();
//                    myAlarm.setMsgNumber(unReadLastDayMsg.getMessageNumber());
//                    myAlarm.setCameFromEmail(true);
//                    myAlarm.setLoadPhoto(true);
//
//
//                    String myContent = getTextFromMessage(unReadLastDayMsg);
//                    EmailParsing.getInstance().parseByModel(camera, mySubject, myAlarm, myContent, context);
//
//
//                    sendLiveAlarm(camera, context);
//                    // Send notification and log the transition details.
//                    //createNotificationChannel(context);
//
//
//                    boolean isMyScreenActivityForeground = getBooleanInPreference(context, IS_MYSCREENACTIVITY_FOREGROUND, false);
//                    if (!isMyScreenActivityForeground) {
//                        sendNotification("new alarm detected", context);
//                    }
//
//                    Intent inn = new Intent(DETECT_ALARM_KEY);
//                    context.sendBroadcast(inn);
//
//
//                    //get attached picture if exist
//                    List<String> attachments = getAttachedFiles(unReadLastDayMsg, context);
//
//                    //in version 1 support only one attached picture
//                    //do not wait to save photo ,to make the process of showing alarm more faster
//                    myAlarm.updateAlarm(attachments.get(0), context);
//                    inn = new Intent(ADD_ATTACHED_PHOTOS_KEY);
//                    context.sendBroadcast(inn);
//                }
//                //update the newest email date
//                lastEmailDate.setTime(unReadLastDayMsgs[0].getReceivedDate());
//                setLongInPreference(context, LAST_DATE_ALARM, lastEmailDate.getTimeInMillis());
//            }
//
//        } catch (MessagingException e) {
//            Log.d("textDate", "exception" + e.getMessage());
//            writeFile("readeLastDayUnreadEmails", context);
//            e.printStackTrace();
//            closeConnection(store, context);
//        }
//
//        //close connection
//        closeConnection(store, context);
//    }
//
//    private void closeConnection(Store store, Context context) {
//        try {
//            if (store != null) {
//                store.close();
//            }
//        } catch (MessagingException e) {
//            writeFile("MessagingException", context);
//            e.printStackTrace();
//        }
//    }
//
//    //get the camera with the same email from and the same model if exist
//    private Camera getAppropriateCamera(Message unReadLastDayMsg, Context context, String[] arrMySubject) {
//
//        ArrayList<Camera> cameras = getSensorsFromLocally(context);
//
//        ListIterator<Camera> iteratorList = cameras.listIterator();
//        while (iteratorList != null && iteratorList.hasNext()) {
//            Camera cameraItem = iteratorList.next();
//
//            //if the camera is not active do not execute arm
//            if (!cameraItem.isArmed()) {
//                //go to next camera
//                continue;
//            }
//
//            //check if the email from is identity to email from that setting in camera
//            boolean isEmailFromIsIdentity = isEmailFromIsIdentity(cameraItem, unReadLastDayMsg, context);
//            if (!isEmailFromIsIdentity) {
//                //go to next camera
//                continue;
//            }
//
//            boolean isEmailModelIsIdentity = isEmailModelIsIdentity(unReadLastDayMsg, cameraItem, arrMySubject, context);
//            if (isEmailModelIsIdentity) {
//                return cameraItem;
//            }
//
//        }
//        return null;
//
//    }
//
//    //check if the model is identity to model that setting in camera
//    private boolean isEmailModelIsIdentity(Message unReadLastDayMsg, Camera camera, String[] arrMySubject, Context context) {
//        String mySubject = null;
//        try {
//            mySubject = unReadLastDayMsg.getSubject();
//        } catch (MessagingException e) {
//            writeFile("isEmailModelIsIdentity", context);
//            e.printStackTrace();
//        }
//
//
//        //filter the message with specify models
//        if (camera.getCameraModel() != null) {
//            //if the mode is without "-"
//            String shortModel1 = camera.getCameraModel().replace("-", "");
//            String shortModel2 = camera.getCameraModel().replaceFirst("-", "");
//
//
//            if (mySubject.contains(camera.getCameraModel())
//                    || mySubject.contains(shortModel1)
//                    || mySubject.contains(shortModel2)
//                    || (camera.getCameraModel().equals("HIKVISION") && mySubject.contains("Network Video Recorder"))
//            ) {
//
//                //in HIKVISION you do'nt need to specify the subject
//                if (!camera.getCameraModel().equals("HIKVISION")) {
//                    int index = mySubject.indexOf(camera.getCameraModel());
//                    if (index == -1) {
//                        index = mySubject.indexOf(shortModel1);
//                    }
//                    if (index == -1) {
//                        index = mySubject.indexOf(shortModel2);
//                    }
//                    //if(index!=-1)
//                    mySubject = mySubject.substring(index);
//                    arrMySubject[0] = mySubject;
//                    return true;
//                }
//
//            }
//        }
//        arrMySubject[0] = "-1";
//        return false;
//    }
//
//    //check if the email from is identity to email from that setting in camera
//    private boolean isEmailFromIsIdentity(Camera cameraItem, Message unReadLastDayMsg, Context context) {
//        if (cameraItem.getEmailAddress() != null) {
//            Address[] address = new Address[0];
//            try {
//                address = unReadLastDayMsg.getFrom();
//            } catch (MessagingException e) {
//                writeFile("isEmailFromIsIdentity", context);
//                e.printStackTrace();
//            }
//            if (address.length > 0) {
//
//                //if the address from is the same as defined
//                String addressFrom = ((InternetAddress) address[0]).getAddress();
//
//                return Objects.equals(cameraItem.getEmailAddress(), addressFrom);
//
//            }
//        }
//        return false;
//    }
//
//    //Get all mails in Inbox Folder
//    private Folder getInbox(Store store, Context context) {
//        //Get all mails in Inbox Folder
//        Folder inbox = null;
//        try {
//            if (store != null) {
//                inbox = store.getFolder("Inbox");
//            }
//        } catch (MessagingException e) {
//            writeFile("isEmailFromIsIdentity", context);
//            e.printStackTrace();
//            return null;
//        }
//        try {
//            if (inbox != null) {
//                inbox.open(Folder.READ_WRITE);
//            }
//        } catch (MessagingException e) {
//            writeFile("isEmailFromIsIdentity", context);
//            e.printStackTrace();
//            return null;
//        }
//        return inbox;
//    }
//
//    //get IMAP session
//    private Session getImapSession(MyEmailAccount myEmailAccount, Properties props) {
//        //Set host address
//        props.setProperty("mail.imap.host", myEmailAccount.getEmailServer());
//
//        //Set specified port
//        if (myEmailAccount.getEmailPort() != null) {
//            props.setProperty("mail.imap.port", myEmailAccount.getEmailPort());
//        }
//
//        //Using SSL
//        if (myEmailAccount.isUseSSL()) {
//            props.setProperty("mail.imap.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
//        }
//
//        props.setProperty("mail.imap.socketFactory.fallback", "false");
//
//
//        //get IMAP session
//        return Session.getInstance(props);
//    }
//
//    //Connect to server by sending username and password.
//    private Store connectToServer(Store store, MyEmailAccount myEmailAccount, Context context) {
//        try {
//            if (store != null) {
//                int port = -1;
//                try {
//                    port = Integer.valueOf(Objects.requireNonNull(myEmailAccount.getEmailPort()));
//                } catch (NumberFormatException ex) {
//                    writeFile("connectToServer", context);
//                    ex.printStackTrace();
//                    return null;
//                }
//                store.connect(myEmailAccount.getEmailServer(), port, myEmailAccount.getEmailAddress(), myEmailAccount.getPassword());
//
//            }
//        } catch (MessagingException e) {
//            e.printStackTrace();
//            writeFile("connectToServer", context);
//            return null;
//        }
//        return store;
//    }
//
//
//    //read camera's unread emails of last day
////    public void readeLastDayUnreadEmails(MyEmailAccount myEmailAccount, Camera camera, Context context) throws IOException {
////
////        Properties props = new Properties();
////        String protocol = null;
////
////        //IMAPS protocol
////        if (myEmailAccount.getEmailServer() != null) {
////            protocol = myEmailAccount.getEmailServer().substring(0, myEmailAccount.getEmailServer().indexOf("."));
////            props.setProperty("mail.store.protocol", protocol);
////        } else {
////            return;
////        }
////
////        //Set host address
////        props.setProperty("mail.imap.host", myEmailAccount.getEmailServer());
////
////        //Set specified port
////        if (myEmailAccount.getEmailPort() != null) {
////            props.setProperty("mail.imap.port", myEmailAccount.getEmailPort());
////        }
////
////        //Using SSL
////        if (myEmailAccount.isUseSSL()) {
////            props.setProperty("mail.imap.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
////        }
////
////        props.setProperty("mail.imap.socketFactory.fallback", "false");
////
////
////        //Setting IMAP session
////        Session imapSession = Session.getInstance(props);
////
////        Store store = null;
////        try {
////            store = imapSession.getStore(protocol);
////        } catch (NoSuchProviderException e) {
////            e.printStackTrace();
////        }
////
////
////        //Connect to server by sending username and password.
////        try {
////            if (store != null) {
////                int port = -1;
////                try {
////                    port = Integer.valueOf(Objects.requireNonNull(myEmailAccount.getEmailPort()));
////                } catch (NumberFormatException ex) {
////                    ex.printStackTrace();
////                    return;
////                }
////
////                store.connect(myEmailAccount.getEmailServer(), port, myEmailAccount.getEmailAddress(), myEmailAccount.getPassword());
////            }
////        } catch (MessagingException e) {
////            e.printStackTrace();
////            Log.d("testConnectMail", "exception connect " + e.getMessage());
////            return;
////        }
////
////        //Get all mails in Inbox Folder
////        Folder inbox = null;
////        try {
////            if (store != null) {
////                inbox = store.getFolder("Inbox");
////            }
////        } catch (MessagingException e) {
////            e.printStackTrace();
////        }
////        try {
////            if (inbox != null) {
////                inbox.open(Folder.READ_WRITE);
////            }
////        } catch (MessagingException e) {
////            e.printStackTrace();
////        }
////
////        //Return result to array of message
////        try {
////            if (inbox != null) {
////
////
////                Calendar cal = Calendar.getInstance();
////                //cal.add(Calendar.MINUTE, -5);
////                cal.roll(Calendar.DATE, false);
////                //cal.roll(Calendar.MINUTE, -2);
////                //String da=getStringFromCalendar(cal,"kk:mm dd/MM/yy",context);
////                //Log.d("textDate",da);
////
////
////                Message[] lastDayMsgs = null;
////                try {
////                    lastDayMsgs = inbox.search(new ReceivedDateTerm(ComparisonTerm.GT, cal.getTime()));
////
////                } catch (Exception ex) {
////                    ex.printStackTrace();
////                    return;
////                }
////
//////                Flags seen = new Flags(Flags.Flag.SEEN);
//////                FlagTerm unseenFlagTerm = new FlagTerm(seen, false);
//////                //Message[] unReadLastDayMsgs = inbox.search(unseenFlagTerm, lastDayMsgs);
////                Message[] unReadLastDayMsgs = inbox.search(new ReceivedDateTerm(ComparisonTerm.GT, cal.getTime()));
////
////                for (Message unReadLastDayMsg : unReadLastDayMsgs) {
////
////                    Log.d("testEmails", unReadLastDayMsg.getSubject());
////
////                    if (!isEmailAlreadyReceived(unReadLastDayMsg.getMessageNumber(), context)) {
////
////                        Address[] address = unReadLastDayMsg.getFrom();
////                        if (address.length > 0) {
////
////                            //if the address from is the same as defined
////                            String addressFrom = ((InternetAddress) address[0]).getAddress();
////
////                            if (!Objects.equals(camera.getEmailAddress(), addressFrom)) {
////                                continue;
////                            }
////
////                            //if the the the interval time between alarms is short and  previous alarm is the same the return (there is another test if the interval time is bigger)
////                            if (checkIfPrevIsSame(prevAlarm, unReadLastDayMsg, addressFrom)) {
////                                continue;
////                            }
////
////                            //set the previous alarm
////                            if (prevAlarm != null) {
////                                prevAlarm.setMsgNumber(unReadLastDayMsg.getMessageNumber());
////                                prevAlarm.setFromEmail(addressFrom);
////                            }
////
////                            //Log.d("testEmails", ((InternetAddress) address[0]).getAddress());
////                        }
////
////                        //cancel the filter of last 5 minutes
//////                    Calendar minDate = Calendar.getInstance();
//////                    minDate.roll(Calendar.MINUTE, -5);
//////                    Calendar maxDate = Calendar.getInstance();
//////
//////                    //filter the message received in the last 5 minutes
//////                    if (unReadLastDayMsg.getReceivedDate().after(minDate.getTime())
//////                            && unReadLastDayMsg.getReceivedDate().before(maxDate.getTime())
//////                            //check if the message is already has been accepted as alarm
//////                            && !isEmailAlreadyReceived(unReadLastDayMsg.getMessageNumber(), context)) {
////
////
////                        //int msgNum=unReadLastDayMsg.getMessageNumber();
////                        String mySubject = unReadLastDayMsg.getSubject();
////
////
////                        //filter the message with specify models
////                        if (camera.getCameraModel() != null) {
////                            //if the mode is without "-"
////                            String shortModel1 = camera.getCameraModel().replace("-", "");
////                            String shortModel2 = camera.getCameraModel().replaceFirst("-", "");
////
////                            //if the camera is not active do not execute arm
////                            if (!camera.isArmed()) {
////                                return;
////                            }
////
////                            if (mySubject.contains(camera.getCameraModel())
////                                    || mySubject.contains(shortModel1)
////                                    || mySubject.contains(shortModel2)
////                                    || (camera.getCameraModel().equals("HIKVISION") && mySubject.contains("Network Video Recorder"))
////                            ) {
////
////                                //in HIKVISION you do'nt need to specify the subject
////                                if (!camera.getCameraModel().equals("HIKVISION")) {
////                                    int index = mySubject.indexOf(camera.getCameraModel());
////                                    if (index == -1) {
////                                        index = mySubject.indexOf(shortModel1);
////                                    }
////                                    if (index == -1) {
////                                        index = mySubject.indexOf(shortModel2);
////                                    }
////                                    //if(index!=-1)
////                                    mySubject = mySubject.substring(index);
////                                }
////
////
////                                //change the message to read email
////                                //inbox.setFlags(new Message[]{unReadLastDayMsg}, new Flags(Flags.Flag.SEEN), true);
////
////                                //Log.d("testSubject", mySubject);
////
////                                Alarm myAlarm = new Alarm();
////                                myAlarm.setMsgNumber(unReadLastDayMsg.getMessageNumber());
////                                myAlarm.setCameFromEmail(true);
////                                myAlarm.setLoadPhoto(true);
////
////
////                                String myContent = getTextFromMessage(unReadLastDayMsg);
////                                EmailParsing.getInstance().parseByModel(camera, mySubject, myAlarm, myContent, context);
////
////
////                                sendLiveAlarm(camera, context);
////                                // Send notification and log the transition details.
////                                //createNotificationChannel(context);
////
////
////                                boolean isMyScreenActivityForeground = getBooleanInPreference(context, IS_MYSCREENACTIVITY_FOREGROUND, false);
////                                if (!isMyScreenActivityForeground) {
////                                    sendNotification("new alarm detected", context);
////                                }
////
////                                Intent inn = new Intent(DETECT_ALARM_KEY);
////                                context.sendBroadcast(inn);
////
////
////                                //get attached picture if exist
////                                List<String> attachments = getAttachedFiles(unReadLastDayMsg, context);
////
////                                //Log.d("checkVideo", attachments.get(0));
////
////                                //in version 1 support only one attached picture
////                                //do not wait to save photo ,to make the process of showing alarm more faster
////                                myAlarm.updateAlarm(attachments.get(0), context);
////                                inn = new Intent(ADD_ATTACHED_PHOTOS_KEY);
////                                context.sendBroadcast(inn);
////                            }
////                        }
////                    }
////                }
////
////            }
////
////        } catch (MessagingException e) {
////            e.printStackTrace();
////        }
////
////        //close connection
////        try {
////            if (store != null) {
////                store.close();
////            }
////        } catch (MessagingException e) {
////            e.printStackTrace();
////        }
////    }
//
//    //if the the the interval time between alarms is short and  previous alarm is the same the return (there is another test if the interval time is bigger)
//    private boolean checkIfPrevIsSame(Alarm prevAlarm, Message unReadLastDayMsg, String addressFrom) {
//        return prevAlarm != null
//                && prevAlarm.getMsgNumber() != null
//                && prevAlarm.getMsgNumber() == unReadLastDayMsg.getMessageNumber()
//                && prevAlarm.getFromEmail() != null
//                && prevAlarm.getFromEmail().equals(addressFrom);
//    }
//
//
////    //read camera's unread emails of last day
////    public void readeLastDayUnreadEmails(Camera camera, Context context) throws IOException {
////
////        Properties props = new Properties();
////        String protocol = null;
////
////        //IMAPS protocol
////        if (camera.getEmailServer() != null) {
////            protocol = camera.getEmailServer().substring(0, camera.getEmailServer().indexOf("."));
////            props.setProperty("mail.store.protocol", protocol);
////        } else {
////            return;
////        }
////
////        //Set host address
////        props.setProperty("mail.imap.host", camera.getEmailServer());
////
////        //Set specified port
////        if (camera.getEmailPort() != null) {
////            props.setProperty("mail.imap.port", camera.getEmailPort());
////        }
////
////        //Using SSL
////        if (camera.isUseSSL()) {
////            props.setProperty("mail.imap.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
////        }
////
////        props.setProperty("mail.imap.socketFactory.fallback", "false");
////
////
////        //Setting IMAP session
////        Session imapSession = Session.getInstance(props);
////
////        Store store = null;
////        try {
////            store = imapSession.getStore(protocol);
////        } catch (NoSuchProviderException e) {
////            e.printStackTrace();
////        }
////
////
////        //Connect to server by sending username and password.
////        try {
////            if (store != null) {
////                int port = -1;
////                try {
////                    port = Integer.valueOf(Objects.requireNonNull(camera.getEmailPort()));
////                } catch (NumberFormatException ex) {
////                    ex.printStackTrace();
////                    return;
////                }
////
////                store.connect(camera.getEmailServer(), port, camera.getEmailAddress(), camera.getPassword());
////            }
////        } catch (MessagingException e) {
////            e.printStackTrace();
////            Log.d("testConnectMail", "exception connect " + e.getMessage());
////            return;
////        }
////
////        //Get all mails in Inbox Folder
////        Folder inbox = null;
////        try {
////            if (store != null) {
////                inbox = store.getFolder("Inbox");
////            }
////        } catch (MessagingException e) {
////            e.printStackTrace();
////        }
////        try {
////            if (inbox != null) {
////                inbox.open(Folder.READ_WRITE);
////            }
////        } catch (MessagingException e) {
////            e.printStackTrace();
////        }
////
////        //Return result to array of message
////        try {
////            if (inbox != null) {
////
////
////                Calendar cal = Calendar.getInstance();
////                //cal.add(Calendar.MINUTE, -5);
////                cal.roll(Calendar.DATE, false);
////                //cal.roll(Calendar.MINUTE, -2);
////                //String da=getStringFromCalendar(cal,"kk:mm dd/MM/yy",context);
////                //Log.d("textDate",da);
////
////
////                Message[] lastDayMsgs = null;
////                try {
////                    lastDayMsgs = inbox.search(new ReceivedDateTerm(ComparisonTerm.GT, cal.getTime()));
////
////                } catch (Exception ex) {
////                    ex.printStackTrace();
////                    return;
////                }
////
//////                Flags seen = new Flags(Flags.Flag.SEEN);
//////                FlagTerm unseenFlagTerm = new FlagTerm(seen, false);
//////                //Message[] unReadLastDayMsgs = inbox.search(unseenFlagTerm, lastDayMsgs);
////                Message[] unReadLastDayMsgs = inbox.search(new ReceivedDateTerm(ComparisonTerm.GT, cal.getTime()));
////
////                for (Message unReadLastDayMsg : unReadLastDayMsgs) {
////
////                    Log.d("testEmails", unReadLastDayMsg.getSubject());
////                    Address[] address = unReadLastDayMsg.getFrom();
////                    if (address.length > 0) {
////                        Log.d("testEmails", ((InternetAddress) address[0]).getAddress());
////                    }
////
////                    Calendar minDate = Calendar.getInstance();
////                    minDate.roll(Calendar.MINUTE, -5);
////                    Calendar maxDate = Calendar.getInstance();
////
////                    //filter to the last 5 minutes
////                    if (unReadLastDayMsg.getReceivedDate().after(minDate.getTime())
////                            && unReadLastDayMsg.getReceivedDate().before(maxDate.getTime())
////                            //check if the message is already has been accepted as alarm
////                            && !isEmailAlreadyReceived(unReadLastDayMsg.getMessageNumber(), context)) {
////
////                        //int msgNum=unReadLastDayMsg.getMessageNumber();
////                        String mySubject = unReadLastDayMsg.getSubject();
////
////
////                        if (camera.getCameraModel() != null) {
////                            //if the mode is without "-"
////                            String shortModel1 = camera.getCameraModel().replace("-", "");
////                            String shortModel2 = camera.getCameraModel().replaceFirst("-", "");
////
////                            //if the camera is not active do not execute arm
////                            if (!camera.isArmed()) {
////                                return;
////                            }
////
////                            if (mySubject.contains(camera.getCameraModel())
////                                    || mySubject.contains(shortModel1)
////                                    || mySubject.contains(shortModel2)
////                                    || (camera.getCameraModel().equals("HIKVISION") && mySubject.contains("Network Video Recorder"))
////                            ) {
////
////                                //in HIKVISION you do'nt need to specify the subject
////                                if (!camera.getCameraModel().equals("HIKVISION")) {
////                                    int index = mySubject.indexOf(camera.getCameraModel());
////                                    if (index == -1) {
////                                        index = mySubject.indexOf(shortModel1);
////                                    }
////                                    if (index == -1) {
////                                        index = mySubject.indexOf(shortModel2);
////                                    }
////                                    //if(index!=-1)
////                                    mySubject = mySubject.substring(index);
////                                }
////
////
////                                //change the message to read email
////                                //inbox.setFlags(new Message[]{unReadLastDayMsg}, new Flags(Flags.Flag.SEEN), true);
////
////                                //Log.d("testSubject", mySubject);
////
////                                Alarm myAlarm = new Alarm();
////                                myAlarm.setMsgNumber(unReadLastDayMsg.getMessageNumber());
////                                myAlarm.setCameFromEmail(true);
////                                myAlarm.setLoadPhoto(true);
////
////                                String myContent = getTextFromMessage(unReadLastDayMsg);
////                                EmailParsing.getInstance().parseByModel(camera, mySubject, myAlarm, myContent, context);
////
////
////                                sendLiveAlarm(camera, context);
////                                // Send notification and log the transition details.
////                                //createNotificationChannel(context);
////
////
////                                sendNotification("new alarm detected", context);
////
////                                Intent inn = new Intent(DETECT_ALARM_KEY);
////                                context.sendBroadcast(inn);
////
////
////                                //get attached picture if exist
////                                List<String> attachments = getAttachedFiles(unReadLastDayMsg, context);
////
////                                //Log.d("checkVideo", attachments.get(0));
////
////                                //in version 1 support only one attached picture
////                                //do not wait to save photo ,to make the process of showing alarm more faster
////                                myAlarm.updateAlarm(attachments.get(0), context);
////                                inn = new Intent(ADD_ATTACHED_PHOTOS_KEY);
////                                context.sendBroadcast(inn);
////                            }
////                        }
////                    }
////                }
////
////            }
////
////        } catch (MessagingException e) {
////            e.printStackTrace();
////        }
////
////        //close connection
////        try {
////            if (store != null) {
////                store.close();
////            }
////        } catch (MessagingException e) {
////            e.printStackTrace();
////        }
////    }
//
//
//
//
//    //send alarm ,catching by map screen
//    private void sendLiveAlarm(Camera camera, Context context) {
//        //send to create alarm :ic_map_main,sound ect...
//        Intent inn = new Intent(CREATE_ALARM_KEY);
//        inn.putExtra(CREATE_ALARM_ID_KEY, camera.getId());
//        inn.putExtra(CREATE_ALARM_NAME_KEY, camera.getName());
//        inn.putExtra(CREATE_ALARM_TYPE_KEY, ALARM_OTHER);
//        context.sendBroadcast(inn);
//    }
//
//
//
//    //save the attached file
//    private List<String> getAttachedFiles(Message message, Context context) throws MessagingException {
//        List<String> attachments = new ArrayList<>();
//        Multipart multipart = null;
//        try {
//            multipart = (Multipart) message.getContent();
//        } catch (IOException | MessagingException e) {
//            writeFile("getAttachedFiles", context);
//            Log.d("testSubject", e.getMessage());
//            e.printStackTrace();
//        }
//
//        if (multipart != null) {
//            for (int i = 0; i < multipart.getCount(); i++) {
//                BodyPart bodyPart = multipart.getBodyPart(i);
//                if (!Part.ATTACHMENT.equalsIgnoreCase(bodyPart.getDisposition()) &&
//                        StringUtils.isBlank(bodyPart.getFileName())) {
//                    continue; // dealing with attachments only
//                }
//                InputStream is = null;
//                try {
//                    is = bodyPart.getInputStream();
//
//                } catch (IOException e) {
//                    writeFile("getAttachedFiles", context);
//                    e.printStackTrace();
//                }
//
//
//                //store in external storage
//                File file = saveImageExternal(is, bodyPart.getFileName(), context);
//                attachments.add(file.getAbsolutePath());
//
//                //store in internal storage
//                //String picName = saveImageInternal(is, bodyPart.getFileName(), context);
//                //attachments.add(picName);
//            }
//        }
//
//        return attachments;
//    }
//
//
////    //save the picture in internal
////    private String saveImageInternal(InputStream is, String fileName, Context context) {
////
////
////        BufferedInputStream bufferedInputStream = new BufferedInputStream(is);
////
////        Bitmap bmp = BitmapFactory.decodeStream(bufferedInputStream);
////
////        //String fullFileName = fileName+".jpg";
////
////        FileOutputStream fileOutputStream;
////        try {
////            fileOutputStream = context.openFileOutput(fileName, Context.MODE_PRIVATE);
////            bmp.compress(Bitmap.CompressFormat.JPEG, 90, fileOutputStream);
////            fileOutputStream.close();
////        } catch (Exception e) {
////            e.printStackTrace();
////        }
////
////        return fileName;
////
////    }
//
//    //save the picture in external
//    private File saveImageExternal(InputStream is, String fileName, Context context) {
//        String root = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString();
//        File myDir = new File(root + "/alarms_hunter");
//        if (!myDir.exists()) {
//            //ignore if the action is successfully
//            myDir.mkdirs();
//        }
//
//        File file = new File(myDir, fileName);
//
//        FileOutputStream fos = null;
//        try {
//            fos = new FileOutputStream(file);
//        } catch (FileNotFoundException e) {
//            writeFile("saveImageExternal", context);
//            e.printStackTrace();
//        }
//        byte[] buf = new byte[4096];
//        int bytesRead = 0;
//        while (true) {
//            try {
//                if (is != null && (bytesRead = is.read(buf)) == -1) break;
//            } catch (IOException e) {
//                writeFile("saveImageExternal", context);
//                e.printStackTrace();
//            }
//            try {
//                if (fos != null) {
//                    fos.write(buf, 0, bytesRead);
//                }
//            } catch (IOException e) {
//                writeFile("saveImageExternal", context);
//                e.printStackTrace();
//            }
//        }
//        try {
//            if (fos != null) {
//                fos.close();
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//        return file;
//    }
//
//    //in android 7 need a white-transparent icon
//    private int getNotificationIcon() {
//        boolean useWhiteIcon = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;
//        if (useWhiteIcon) {
//            return R.drawable.ic_app_notification;
//        } else return R.mipmap.ic_launcher;
//    }
//
//    //snd notification when accept alarm
//    private void sendNotification(String content, Context context) {
//
//
//        if (context == null)
//            return;
//        WeakReference<Context> wContext = new WeakReference<Context>(context);
//
//        createNotificationChannel(wContext.get());
////        if(mNotificationId<28){
////            mNotificationId++
////        }else{
////            mNotificationId=23
////        }
//
//        long oneTimeID = SystemClock.uptimeMillis();
//        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context, CHANNEL_ID)
//                .setSmallIcon(getNotificationIcon())
//                .setContentTitle("New alarm detected")
//                .setContentText(content)
//                //remove after click on notification
//                .setAutoCancel(true);
//
//        NotificationManager mNotificationManager = (NotificationManager) wContext.get().getSystemService(Context.NOTIFICATION_SERVICE);
//
//        //Enable press on notification to open app
//        Intent notificationIntent = new Intent(context, InitAppActivity.class);
//        PendingIntent contentIntent = PendingIntent.getActivity(context, 0, notificationIntent, 0);
//        mBuilder.setContentIntent(contentIntent);
//
//        if (mNotificationManager != null) {
//            mNotificationManager.notify((int) oneTimeID, mBuilder.build());
//        }
//    }
//
//
//    //validation email by trying to connection
//    public boolean emailValidation(Camera camera, Context context) {
//        Properties props = new Properties();
//        String protocol = null;
//
//        //IMAPS protocol
//        if (camera.getEmailServer() != null) {
//            protocol = camera.getEmailServer().substring(0, camera.getEmailServer().indexOf("."));
//            props.setProperty("mail.store.protocol", protocol);
//        } else {
//            return false;
//        }
//
//        //Set host address
//        props.setProperty("mail.imap.host", camera.getEmailServer());
//
//        //Set specified port
//        if (camera.getEmailPort() != null) {
//            props.setProperty("mail.imap.port", camera.getEmailPort());
//        }
//
//        //Using SSL
//        if (camera.isUseSSL()) {
//            props.setProperty("mail.imap.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
//        }
//
//        props.setProperty("mail.imap.socketFactory.fallback", "false");
//
//
//        //Setting IMAP session
//        Session imapSession = Session.getInstance(props);
//
//        Store store = null;
//        try {
//            store = imapSession.getStore(protocol);
//        } catch (NoSuchProviderException e) {
//            writeFile("emailValidation", context);
//            sendErrorMsg(e.getMessage(), context);
//            e.printStackTrace();
//        }
//
//
//        //Connect to server by sending username and password.
//        try {
//            if (store != null) {
//                int port = -1;
//                try {
//                    port = Integer.valueOf(Objects.requireNonNull(camera.getEmailPort()));
//                } catch (NumberFormatException ex) {
//                    writeFile("emailValidation", context);
//                    ex.printStackTrace();
//                    sendErrorMsg(ex.getMessage(), context);
//                    return false;
//                }
//
//                store.connect(camera.getEmailServer(), port, camera.getEmailAddress(), camera.getPassword());
//
//                return store.isConnected();
//            }
//        } catch (MessagingException e) {
//            writeFile("emailValidation", context);
//            e.printStackTrace();
//            sendErrorMsg(e.getMessage(), context);
//            return false;
//        }
//        return false;
//    }
//
//    //validation email by trying to connection
//    public boolean emailValidation(MyEmailAccount myEmailAccount, Context context) {
//        Properties props = new Properties();
//        String protocol = null;
//
//        //IMAPS protocol
//        if (myEmailAccount.getEmailServer() != null) {
//            protocol = myEmailAccount.getEmailServer().substring(0, myEmailAccount.getEmailServer().indexOf("."));
//            props.setProperty("mail.store.protocol", protocol);
//        } else {
//            return false;
//        }
//
//        //Set host address
//        props.setProperty("mail.imap.host", myEmailAccount.getEmailServer());
//
//        //Set specified port
//        if (myEmailAccount.getEmailPort() != null) {
//            props.setProperty("mail.imap.port", myEmailAccount.getEmailPort());
//        }
//
//        //Using SSL
//        if (myEmailAccount.isUseSSL()) {
//            props.setProperty("mail.imap.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
//        }
//
//        props.setProperty("mail.imap.socketFactory.fallback", "false");
//
//
//        //Setting IMAP session
//        Session imapSession = Session.getInstance(props);
//
//        Store store = null;
//        try {
//            store = imapSession.getStore(protocol);
//        } catch (NoSuchProviderException e) {
//            writeFile("emailValidation", context);
//            sendErrorMsg(e.getMessage(), context);
//            e.printStackTrace();
//        }
//
//
//        //Connect to server by sending username and password.
//        try {
//            if (store != null) {
//                int port = -1;
//                try {
//                    port = Integer.valueOf(Objects.requireNonNull(myEmailAccount.getEmailPort()));
//                } catch (NumberFormatException ex) {
//                    ex.printStackTrace();
//                    sendErrorMsg(ex.getMessage(), context);
//                    return false;
//                }
//
//                store.connect(myEmailAccount.getEmailServer(), port, myEmailAccount.getEmailAddress(), myEmailAccount.getPassword());
//
//                return store.isConnected();
//            }
//        } catch (MessagingException e) {
//            writeFile("emailValidation", context);
//            e.printStackTrace();
//            sendErrorMsg(e.getMessage(), context);
//            return false;
//        }
//        return false;
//    }
//
//
//
//    //send error to extra settings screen
//    private void sendErrorMsg(String message, Context context) {
//        Intent inn = new Intent(ERROR_RESULT_VALIDATION_EMAIL_ACTION);
//        inn.putExtra(ERROR_VALIDATION_EMAIL_MSG_KEY, message);
//        context.sendBroadcast(inn);
//    }
//
//
//    //    Notification channels enable us app developers to group our notifications into groups—channels—with
////    the user having the ability to modify notification settings for the entire channel at once. For example,
////    for each channel, users can completely block all notifications, override the importance level, or allow a
////    notification badge to be shown. This new feature helps in greatly improving the user experience of an app
//    private void createNotificationChannel(Context context) {
//        if (context == null)
//            return;
//
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            String name = CHANNEL_NAME;
//            String descriptionText = "new alarm detected";
//            int importance = NotificationManager.IMPORTANCE_DEFAULT;
//            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
//            channel.setDescription(descriptionText);
//            // Register the channel with the system; you can't change the importance
//            // or other notification behaviors after this
//            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
//            if (notificationManager != null) {
//                notificationManager.createNotificationChannel(channel);
//            }
//        }
//    }
//
//
//    private String getTextFromMessage(Message message) throws MessagingException, IOException {
//        String result = "";
//        if (message.isMimeType("text/plain")) {
//            result = message.getContent().toString();
//        } else if (message.isMimeType("text/html")) { // **
//            result = message.getContent().toString(); // **
//        } else if (message.isMimeType("multipart/*")) {
//            MimeMultipart mimeMultipart = (MimeMultipart) message.getContent();
//            result = getTextFromMimeMultipart(mimeMultipart);
//        }
//        return result;
//    }
//
//    private String getTextFromMimeMultipart(
//            MimeMultipart mimeMultipart) throws MessagingException, IOException {
//        String result = "";
//        int count = mimeMultipart.getCount();
//        for (int i = 0; i < count; i++) {
//            BodyPart bodyPart = mimeMultipart.getBodyPart(i);
//            if (bodyPart.isMimeType("text/plain")) {
//                result = result + "\n" + bodyPart.getContent();
//                break; // without break same text appears twice in my tests
//            } else if (bodyPart.isMimeType("text/html")) {
//                String html = (String) bodyPart.getContent();
//                result = result + "\n" + org.jsoup.Jsoup.parse(html).text();
//            } else if (bodyPart.getContent() instanceof MimeMultipart) {
//                result = result + getTextFromMimeMultipart((MimeMultipart) bodyPart.getContent());
//            }
//        }
//        return result;
//    }
//
//    //check if this message already accepted as alarm
//    private boolean isEmailAlreadyReceived(int msgNum, Context context) {
//        //val alarms = context.get()?.let { populateAlarmsFromLocally(it) }
//        ArrayList<Alarm> alarms = getAlarmsFromLocally(context);
//
//        if (alarms == null) {
//            return false;
//        }
//
//        ListIterator<Alarm> iteratorList = alarms.listIterator();
//
//        while (iteratorList != null && iteratorList.hasNext()) {
//            Alarm item = iteratorList.next();
//            if (item.getMsgNumber() != null && item.getMsgNumber() == msgNum) {
//                return true;
//            }
//        }
//        return false;
//    }
//}
