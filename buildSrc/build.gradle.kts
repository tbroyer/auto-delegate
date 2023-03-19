plugins {
    `kotlin-dsl`
    alias(libs.plugins.spotless)
}

dependencies {
    implementation(plugin(libs.plugins.errorprone))
    implementation(plugin(libs.plugins.nullaway))
    implementation(plugin(libs.plugins.spotless))
}

spotless {
    kotlinGradle {
        target("*.gradle.kts", "src/main/kotlin/**/*.gradle.kts")
        ktlint(libs.versions.ktlint.get())
    }
}

fun DependencyHandlerScope.plugin(plugin: Provider<PluginDependency>) =
    plugin.map { "${it.pluginId}:${it.pluginId}.gradle.plugin:${it.version}" }
