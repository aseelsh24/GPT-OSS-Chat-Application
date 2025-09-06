# Add project specific ProGuard rules here.
-keep class com.gptoss.chat.** { *; }
-keep class retrofit2.** { *; }
-keep class okhttp3.** { *; }
-keep class com.google.gson.** { *; }

-dontwarn retrofit2.**
-dontwarn okhttp3.**
-dontwarn okio.**

-keepattributes Signature
-keepattributes *Annotation*

-keep class * implements com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

-keepclassmembers,allowobfuscation class * {
  @com.google.gson.annotations.SerializedName <fields>;
}
