plugins {
    id("local.java-library")
    id("local.maven-publish")
}

base.archivesName.set("auto-delegate-processor")

nullaway {
    annotatedPackages.add("net.ltgt.auto.delegate.processor")
}

dependencies {
    compileOnly(libs.checkerQual)
    implementation(libs.javapoet)

    compileOnly(libs.autoService.annotations)
    annotationProcessor(libs.autoService)

    compileOnly(libs.incap)
    annotationProcessor(libs.incap.processor)
}

testing {
    suites {
        withType<JvmTestSuite> {
            dependencies {
                // We could use compileOnlyApi above, but we don't want the dependency in the POM.
                // This is OK because annotation processors aren't regular Java libraries you compile against.
                compileOnly(libs.checkerQual)
                compileOnly(libs.autoService.annotations)
                compileOnly(libs.incap)
            }
        }
        named<JvmTestSuite>("test") {
            dependencies {
                implementation(projects.annotations)
                implementation(libs.compileTesting)
                implementation(libs.truth)
            }
            targets.configureEach {
                testTask.configure {
                    jvmArgs(
                        "--add-opens=jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED",
                        "--add-opens=jdk.compiler/com.sun.tools.javac.main=ALL-UNNAMED",
                        "--add-opens=jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED",
                    )
                }
            }
        }
    }
}
