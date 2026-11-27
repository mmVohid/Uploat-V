# Xato hisobotlarida (nusxalangan stack trace) qator raqamlari ko'rinib tursin
-keepattributes SourceFile,LineNumberTable,Signature,InnerClasses,EnclosingMethod,*Annotation*
-renamesourcefileattribute SourceFile

# yt-dlp kutubxonasi: Jackson orqali JSON'ni reflection bilan o'qiydi, native kod JNI orqali chaqiriladi
-keep class com.yausername.** { *; }
-keep class com.fasterxml.jackson.** { *; }
-keepclassmembers class * { native <methods>; }

# Ixtiyoriy bog'liqliklar (commons-compress va boshqalar) ilovada ishlatilmaydi
-dontwarn org.tukaani.**
-dontwarn com.github.luben.**
-dontwarn org.brotli.**
-dontwarn org.objectweb.asm.**
-dontwarn javax.naming.**
-dontwarn java.lang.management.**
-dontwarn org.apache.commons.compress.**
-dontwarn org.slf4j.**
-dontwarn com.fasterxml.jackson.databind.**
