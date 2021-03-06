-keepattributes *Annotation*
-keep class twitter4j.** { *; }
-keep class com.mobeta.android.dslv.** { *; }
-keep class com.twitter.twittertext.** { *; }
-keep class com.fasterxml.jackson.** { *; }
-dontwarn twitter4j.**
-dontwarn com.mobeta.android.dslv.**
-dontwarn com.twitter.twittertext.**
-dontwarn com.fasterxml.jackson.**

# Crashlytics
-keepattributes SourceFile,LineNumberTable
-keep public class * extends java.lang.Exception
