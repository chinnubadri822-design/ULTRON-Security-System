package com.ultron.security

import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.view.Gravity
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.*
import java.util.Locale

class MainActivity : Activity(), TextToSpeech.OnInitListener {
 private val bg=Color.rgb(4,9,20); private val cyan=Color.rgb(0,220,255); private val panel=Color.rgb(12,24,43)
 private lateinit var console:LinearLayout; private lateinit var input:EditText; private lateinit var status:TextView
 private lateinit var tts:TextToSpeech; private var speech:SpeechRecognizer?=null
 override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);window.statusBarColor=bg;window.navigationBarColor=bg;tts=TextToSpeech(this,this)
 if(Build.VERSION.SDK_INT>=26)getSystemService(NotificationManager::class.java).createNotificationChannel(NotificationChannel(SecurityReceiver.CHANNEL_ID,"JARVIS security alerts",NotificationManager.IMPORTANCE_HIGH))
 if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS),70)
 home()}
 override fun onInit(r:Int){if(r==TextToSpeech.SUCCESS)tts.language=Locale.getDefault()}
 private fun shape(c:Int)=GradientDrawable().apply{setColor(c);cornerRadius=18f}
 private fun text(s:String,n:Float,c:Int=Color.WHITE,b:Boolean=false)=TextView(this).apply{text=s;textSize=n;setTextColor(c);if(b)setTypeface(typeface,Typeface.BOLD)}
 private fun home(){
  val root=LinearLayout(this).apply{orientation=1;setPadding(18,18,18,12);setBackgroundColor(bg)}
  val top=LinearLayout(this).apply{gravity=Gravity.CENTER_VERTICAL}
  top.addView(text("J.A.R.V.I.S.",26f,cyan,true),LinearLayout.LayoutParams(0,-2,1f))
  top.addView(Button(this).apply{text="SETTINGS";setTextColor(cyan);setBackgroundColor(Color.TRANSPARENT);setOnClickListener{settings()}})
  root.addView(top);root.addView(text("PERSONAL DEVICE SECURITY SYSTEM",10f,Color.LTGRAY))
  root.addView(text("◉",70f,cyan,true).apply{gravity=Gravity.CENTER},LinearLayout.LayoutParams(-1,105))
  status=text("SYSTEM READY • OFFLINE MODE",12f,cyan,true).apply{gravity=Gravity.CENTER;setPadding(0,0,0,10)};root.addView(status)
  val sc=ScrollView(this);val body=LinearLayout(this).apply{orientation=1}
  body.addView(card("APP SECURITY SCAN","Review installed apps for sensitive permissions. This is not a malware verdict.","SCAN APPS"){scan()})
  body.addView(card("NEW APP WATCH","JARVIS can alert after an app is installed. Android does not allow ordinary apps to block the installer beforehand.","NOTIFICATION SETTINGS"){startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE,packageName))})
  body.addView(card("EMERGENCY SOS","Open the dialer and confirm the emergency call yourself.","OPEN DIALER"){startActivity(Intent(Intent.ACTION_DIAL,Uri.parse("tel:112")))})
  body.addView(card("VOICE ASSISTANT","Use basic offline commands. No API key required.","TALK TO JARVIS"){voice()})
  body.addView(text("JARVIS CONSOLE",15f,cyan,true).apply{setPadding(0,12,0,5)})
  console=LinearLayout(this).apply{orientation=1;background=shape(panel);setPadding(12,12,12,12)}
  console.addView(text("JARVIS: Online. I can review app permissions and open emergency tools. I cannot guarantee detection of every threat.",13f));body.addView(console)
  val row=LinearLayout(this).apply{gravity=Gravity.CENTER_VERTICAL}
  input=EditText(this).apply{hint="Ask JARVIS...";setHintTextColor(Color.GRAY);setTextColor(Color.WHITE);setSingleLine(true);background=shape(panel)}
  row.addView(input,LinearLayout.LayoutParams(0,50,1f));row.addView(Button(this).apply{text="SEND";setTextColor(bg);background=shape(cyan);setOnClickListener{reply()}})
  body.addView(row);sc.addView(body);root.addView(sc,LinearLayout.LayoutParams(-1,0,1f));setContentView(root)
 }
 private fun card(t:String,d:String,b:String,fn:()->Unit):View{val l=LinearLayout(this).apply{orientation=1;background=shape(panel);setPadding(14,12,14,12)}
 l.addView(text(t,17f,cyan,true));l.addView(text(d,12f,Color.LTGRAY).apply{setPadding(0,5,0,8)})
 l.addView(Button(this).apply{text=b;setTextColor(bg);background=shape(cyan);setOnClickListener{fn()}});return l}
 private fun line(w:String,s:String){console.addView(text("$w: $s",13f).apply{setPadding(0,5,0,5)})}
 private fun reply(){val q=input.text.toString().trim().lowercase(Locale.ROOT);if(q.isEmpty())return;input.setText("")
 (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(input.windowToken,0);line("YOU",q)
 val a=when{ "scan" in q->{scan();"Starting the app permission review."};"sos" in q||"emergency" in q->"Use the SOS card to open the dialer and confirm the call."
 "unknown app" in q||"new app" in q->"I can alert after installation, but cannot block Android's installer."
 "permission" in q->"Review camera, microphone, contacts, SMS and location permissions in Android Settings."
 "phishing" in q||"link" in q->"Avoid unexpected links. Verify the sender and website before entering information."
 "hello" in q||"hi" in q->"Hello. JARVIS offline systems are ready."
 else->"I am offline. Try: scan apps, unknown app, permissions, phishing or emergency."}
 line("JARVIS",a);tts.speak(a,TextToSpeech.QUEUE_FLUSH,null,"jarvis")}
 private fun scan(){status.text="SCANNING APP PERMISSIONS..."
 val map=mapOf("android.permission.READ_SMS" to "read SMS","android.permission.SEND_SMS" to "send SMS","android.permission.READ_CONTACTS" to "contacts","android.permission.RECORD_AUDIO" to "microphone","android.permission.CAMERA" to "camera","android.permission.ACCESS_FINE_LOCATION" to "precise location","android.permission.READ_CALL_LOG" to "call history")
 val found=mutableListOf<String>()
 try{val list=if(Build.VERSION.SDK_INT>=33)packageManager.getInstalledPackages(PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong())) else {
 @Suppress("DEPRECATION") packageManager.getInstalledPackages(PackageManager.GET_PERMISSIONS)}
 for(p in list){val a=p.applicationInfo?:continue;if((a.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0)continue
 val perms=(p.requestedPermissions?: emptyArray()).filter{map.containsKey(it)}.mapNotNull{map[it]}.distinct()
 if(perms.isNotEmpty())found.add(packageManager.getApplicationLabel(a).toString()+" — "+perms.joinToString(", "))}}
 catch(_:Exception){found.add("Could not read installed apps on this device.")}
 status.text="REVIEW COMPLETE • "+found.size+" APP(S) WITH SENSITIVE PERMISSIONS"
 val report=if(found.isEmpty())"No listed sensitive permissions found. This does not prove the device is malware-free." else found.joinToString("\n\n")
 AlertDialog.Builder(this).setTitle("JARVIS APP REVIEW").setMessage(report).setPositiveButton("OK",null).setNeutralButton("APP SETTINGS"){_,_->startActivity(Intent(Settings.ACTION_APPLICATION_SETTINGS))}.show()}
 private fun settings(){val items=arrayOf("JARVIS app permissions","Android security settings","About JARVIS")
 AlertDialog.Builder(this).setTitle("JARVIS SETTINGS").setItems(items){_,i->when(i){0->startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:$packageName")));1->startActivity(Intent(Settings.ACTION_SECURITY_SETTINGS));2->AlertDialog.Builder(this).setMessage("JARVIS is an offline helper, not a full antivirus or guaranteed emergency monitor.").setPositiveButton("OK",null).show()}}.show()}
 private fun voice(){if(!SpeechRecognizer.isRecognitionAvailable(this)){Toast.makeText(this,"Speech recognition unavailable",Toast.LENGTH_LONG).show();return}
 if(Build.VERSION.SDK_INT>=23&&checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO),41);return}
 speech?.destroy();speech=SpeechRecognizer.createSpeechRecognizer(this)
 speech?.setRecognitionListener(object:android.speech.RecognitionListener{
 override fun onReadyForSpeech(p:Bundle?){status.text="LISTENING..."};override fun onBeginningOfSpeech(){};override fun onRmsChanged(v:Float){};override fun onBufferReceived(b:ByteArray?){};override fun onEndOfSpeech(){}
 override fun onError(e:Int){status.text="SYSTEM READY • OFFLINE MODE"};override fun onResults(b:Bundle?){val s=b?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull();if(!s.isNullOrBlank()){input.setText(s);reply()};status.text="SYSTEM READY • OFFLINE MODE"}
 override fun onPartialResults(b:Bundle?){};override fun onEvent(t:Int,b:Bundle?){}})
 speech?.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM).putExtra(RecognizerIntent.EXTRA_LANGUAGE,Locale.getDefault()))}
 override fun onRequestPermissionsResult(r:Int,p:Array<out String>,g:IntArray){super.onRequestPermissionsResult(r,p,g);if(r==41&&g.firstOrNull()==PackageManager.PERMISSION_GRANTED)voice()}
 override fun onDestroy(){speech?.destroy();tts.stop();tts.shutdown();super.onDestroy()}
}