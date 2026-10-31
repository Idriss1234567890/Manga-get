# مصدر MangaLix — مانجا ليك

الموقع: https://mangalik.net

## الدوال
- `getPopular(page)` قائمة المانجا.
- `search(query, page)` البحث.
- `getDetails(url)` العنوان والغلاف والقصة والتصنيفات والحالة.
- `getChapters(url)` روابط الفصول.
- `getPageImages(url)` صور صفحات الفصل.

## ما أثبتته ملفات HTML المرسلة
- صفحة التفاصيل: `https://mangalik.net/manga/i-bought-the-villains-with-money/`
- صفحة الفصل 20: `https://mangalik.net/manga/i-bought-the-villains-with-money/20/`
- صفحة الفصل تحتوي صوراً عبر `img.wp-manga-chapter-img` داخل `.reading-content`، وتم العثور على 8 صور.
- صفحة التفاصيل تحتوي عنوان المانهوا والغلاف والقصة والتصنيفات والحالة.
- ملف التفاصيل المرسل لا يحتوي قائمة فصول ظاهرة؛ لذلك محددات `getChapters` مبنية على بنية Madara/WP Manga المعتادة وتحتاج اختباراً على صفحة حية.

## الدمج
هذه وحدة Kotlin/JVM مستقلة وليست APK إضافة جاهزة لـ Mihon أو Kotatsu. يمكن إضافتها كوحدة Gradle أو نقل ملفات Kotlin إلى مشروع Android.

```kotlin
implementation("org.jsoup:jsoup:1.18.3")
implementation("com.squareup.okhttp3:okhttp:4.12.0")
```

مثال:
```kotlin
val source = MangalekSource()
val manga = source.getPopular().first()
val details = source.getDetails(manga.url)
val chapters = source.getChapters(details.url)
val images = chapters.firstOrNull()?.let { source.getPageImages(it.url) }.orEmpty()
```

## حدود الوصول
لا يتجاوز هذا المصدر CAPTCHA أو Cloudflare أو تسجيل الدخول. إذا أعاد الموقع صفحة تحقق، سيظهر خطأ بدلاً من محاولة تجاوز الحماية. قد تتغير محددات HTML إذا غيّر الموقع تصميمه. ارفع الكود فقط إلى GitHub، ولا ترفع صور الفصول أو نسخاً من محتوى الموقع.
