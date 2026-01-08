MicWarmer
=========
![logo: burning smd microphone](/app/src/main/res/mipmap-xxhdpi/ic_launcher.webp)

This project is meant to fix an issue that I have with my phone.

The [digital MEMS microphone](https://www.st.com/en/mems-and-sensors/mems-microphones.html)
does not work properly anymore; when receiving a call (or making a recording), there is a very 
loud tone that modulates a little. After a minute or so, the microphone starts to work correctly. It
turns out that my phone turns off the power to the mic when it is not in use and that the mic is 
defective.

Given that the mic needs to "warm up" before I can use it to receive phone calls, I created this app 
that turns the mic on all the time by using the [AudioRecord](https://developer.android.com/reference/android/media/AudioRecord) 
Android API and then I discard the audio data.

On most phones the microphone is soldered to the charging port board, which is easy and cheap to get 
and replace. Otherwise, replacing the smd component is possible with the adequate equipment. 
All of my donor phones have analog microphones.

The impact on the battery life is less than 5% in a day.

This project is for my Realme 8 5G. It might not work on other devices for several reasons:

* The app does not ask for notification permission. In theory, it is mandatory to show a high 
  priority notification when using foreground services (and it is needed to record audio). but it 
  turns out if you don't have the permission, it works anyway, with no notification. This might not 
  work on all phones, try allowing notifications for the app.
* The audio recording settings that I choose might not work for all phones (it should default to 
  some working defaults, but who knows).
* In the audio recording thread I have a 10 seconds Thread.sleep between read calls. This might 
  be a problem, I can't control what the HAL or the device driver does. It works on my phone.

![screenshot](/screen.png)