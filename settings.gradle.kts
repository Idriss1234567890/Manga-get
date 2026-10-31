pluginManagement { repositories { google(); mavenCentral(); gradlePluginPortal() } }
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories { google(); mavenCentral() }
}
rootProject.name = "MangaLix"

include(":core", ":app")

// كل مجلد في جذر المستودع يحوي source.json يُعدّ وحدة مصدر ويُضمّ تلقائياً (mangalek/ وغيره).
// إضافة موقع جديد = إضافة مجلد جديد فقط، دون تعديل هذا الملف ولا كود التطبيق.
rootDir.listFiles()
    ?.filter { it.isDirectory && File(it, "source.json").isFile }
    ?.sortedBy { it.name }
    ?.forEach { include(":${it.name}") }
