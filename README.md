# MangaLix Sources — Mangalek

مستودع أولي لمصادر MangaLix. يحتوي وحدة Kotlin لموقع مانجا ليك.

ابدأ من `mangalek/README.md`.

الحالة: نسخة تجريبية؛ استخراج تفاصيل المانهوا وصور الفصل مبني على ملفات HTML المقدمة. استخراج قائمة الفصول يحتاج اختباراً إضافياً.

---

# MangaLix Reader

تطبيق Android (Kotlin + Jetpack Compose، واجهة عربية RTL) لقراءة المانغا من مصادر مستقلة. النص أعلاه هو README المستودع الأصلي وقد أُبقي كما هو.

## الهيكل

```
core/                 الواجهة MangaSource + النماذج + SourceHttpClient + الأخطاء + السجل + فهرس المستودع
app/                  التطبيق (Compose، Room، Coil، شاشات: الرئيسية/المكتبة/المصادر/التفاصيل/القارئ)
mangalek/             مصدر مانجا ليك (الكود القديم محفوظ + MangalekMangaSource الجديد)
repository.json       فهرس المصادر لمستودع GitHub
tools/check_repository.py   فحص اتساق الفهرس (يعمل في CI)
```

## كيف يعمل نظام المصادر

- كل مصدر وحدة Gradle في مجلد مستقل في جذر المستودع، وعلامته وجود `source.json`.
- `settings.gradle.kts` و`app/build.gradle.kts` يضمّان كل مجلد فيه `source.json` تلقائياً.
- المصدر يسجّل نفسه بصنف `SourceFactory` في `META-INF/services` ويكتشفه `SourceRegistry` وقت التشغيل.
- لا يُنزَّل كود Kotlin ويُشغَّل على الجهاز. `repository.json` فهرس يعرض المصادر المتاحة وإصداراتها ويتحقق من توافق `apiVersion` وسلامة `source.json`؛ أما تضمين مصدر جديد أو تحديثه فيتم بالبناء (أو بتحديث التطبيق).

## إضافة مصدر جديد

1. أنشئ مجلداً في الجذر، مثل `mysite/`.
2. `mysite/source.json`:
   ```json
   {
     "id": "mysite", "name": "اسم الموقع", "language": "ar",
     "version": "1.0.0", "apiVersion": 1, "baseUrl": "https://example.org",
     "headers": { "userAgent": "…", "referer": "https://example.org/", "extra": {} }
   }
   ```
   `headers` اختيارية؛ ما لم يُذكر يأخذ قيمة افتراضية. `extra` ترويسات إضافية تتغلب على الافتراضية.
3. `mysite/build.gradle.kts`: انسخ `mangalek/build.gradle.kts` وغيّر اسم ملف JSON المنسوخ (`rename { "mysite.json" }`).
4. `mysite/src/main/kotlin/...`: صنف ينفّذ `MangaSource` (الدوال الست: `getPopular`, `getLatest`, `search`, `getDetails`, `getChapters`, `getPages`) فوق `SourceHttpClient`، ويُفضَّل فصل المحلل عن الشبكة كما في `MangalekParser` ليسهل اختباره.
5. صنف `SourceFactory` (انظر `MangalekSourceFactory`) وملف `src/main/resources/META-INF/services/com.mangalix.core.registry.SourceFactory` يحوي اسمه الكامل.
6. أضف مدخلاً في `repository.json` (`id`, `name`, `language`, `version`, `apiVersion`, `manifest`: "mysite/source.json").
7. اختبارات: ملفات HTML في `src/test/resources/fixtures/` واختبارات تستخدم `FixtureFetcher` (انظر `MangalekSourceTest`).
8. `python3 tools/check_repository.py` ثم `./gradlew :mysite:test :app:assembleDebug`.

لا حاجة لتعديل أي ملف في `app/` أو `core/`.

## البناء

```
./gradlew :core:test :mangalek:test      # اختبارات بلا إنترنت
./gradlew :app:assembleDebug             # app/build/outputs/apk/debug/app-debug.apk
```
يتطلب JDK 17 وAndroid SDK 35. نفس الخطوات تعمل تلقائياً في GitHub Actions (`.github/workflows/build.yml`) ويُرفع APK كـ artifact.

## حالة الاختبار (بصدق)

- `core` و`mangalek` (منطق المحلل، العميل، الأخطاء، السجل، الفهرس): اختباراتهما تعمل على ملفات HTML محلية.
- ملفات HTML في `mangalek/src/test/resources/fixtures/` **كُتبت يدوياً** لتحاكي قالب Madara وليست نسخاً ملتقطة من الموقع.
- استخراج الفصول عبر AJAX (`/ajax/chapters/` ثم `admin-ajax.php`) وروابط «آخر التحديثات» (`m_orderby=latest`) مبنية على سلوك Madara القياسي ولم تُجرَّب على الموقع الحي بعد.
- يعرض التطبيق رسالة عربية عند التحقق الأمني (Cloudflare/CAPTCHA) ولا يحاول تجاوزه.

