plugins {
    id("local.base")
    `java-library`
    id("net.ltgt.errorprone")
    id("net.ltgt.nullaway")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(project.the<VersionCatalogsExtension>().named("libs").findVersion("javaToolchain").orElseThrow().requiredVersion))
    }
}

tasks {
    compileJava {
        options.encoding = "UTF-8"
        options.compilerArgs.addAll(arrayOf("-Werror", "-Xlint:all,-fallthrough,-processing"))
        options.release.set(project.the<VersionCatalogsExtension>().named("libs").findVersion("targetJavaRelease").orElseThrow().requiredVersion.toInt())
    }
    withType<Jar> {
        // Don't include the version in the JAR name for a stable filename, to avoid clutter in buildDir
        archiveVersion.set(null as String?)
    }
}

dependencies {
    errorprone(project.the<VersionCatalogsExtension>().named("libs").findBundle("errorprone").orElseThrow())
}

testing.suites.withType<JvmTestSuite>().configureEach {
    useJUnit(project.the<VersionCatalogsExtension>().named("libs").findVersion("junit").orElseThrow().requiredVersion)
}

spotless {
    java {
        googleJavaFormat(project.the<VersionCatalogsExtension>().named("libs").findVersion("googleJavaFormat").orElseThrow().requiredVersion)
        licenseHeaderFile(rootProject.file("LICENSE.header"))
    }
}
