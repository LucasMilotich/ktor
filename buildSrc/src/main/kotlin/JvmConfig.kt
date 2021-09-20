/*
 * Copyright 2014-2021 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */
import org.gradle.api.*
import org.gradle.api.tasks.testing.*
import org.gradle.jvm.tasks.*
import org.gradle.kotlin.dsl.*
import org.jetbrains.kotlin.gradle.targets.jvm.tasks.*

val JDK_16: String? = System.getProperty("JDK_16")
val JDK_17: String? = System.getProperty("JDK_17")
val JDK_18: String? = System.getProperty("JDK_18")

val JDK_11: String? = System.getProperty("JDK_11")

fun Project.configureJvm() {
    val jdk = when (name) {
        in jdk11Modules -> 11
        in jdk8Modules -> 8
        in jdk7Modules -> 7
        else -> 6
    }

    val kotlin_version: String by extra
    val slf4j_version: String by extra
    val junit_version: String by extra
    val coroutines_version: String by extra

    val configuredVersion: String by rootProject.extra

    kotlin {
        jvm()

        sourceSets.apply {
            val jvmMain by getting {
                dependencies {
                    api("org.jetbrains.kotlin:kotlin-stdlib:$kotlin_version")
                    if (jdk > 6) {
                        api("org.jetbrains.kotlin:kotlin-stdlib-jdk7:$kotlin_version")
                    }
                    if (jdk > 7) {
                        api("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlin_version")
                        api("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:$coroutines_version") {
                            exclude(module = "kotlin-stdlib")
                            exclude(module = "kotlin-stdlib-jvm")
                            exclude(module = "kotlin-stdlib-jdk8")
                            exclude(module = "kotlin-stdlib-jdk7")
                        }
                    }

                    api("org.slf4j:slf4j-api:$slf4j_version")
                }
            }

            val jvmTest by getting {
                dependencies {
                    api("org.jetbrains.kotlin:kotlin-test")
                    api("org.jetbrains.kotlin:kotlin-test-junit")
                    api("junit:junit:$junit_version")

                    api("org.jetbrains.kotlin:kotlin-test-junit:$kotlin_version")
                    api("junit:junit:$junit_version")

                    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-debug:$coroutines_version")
                }
            }
        }
    }

    tasks.register<Jar>("jarTest") {
        dependsOn(tasks.getByName("jvmTestClasses"))
        classifier = "test"
        from(kotlin.targets.getByName("jvm").compilations.getByName("test").output)
    }

    configurations.apply {
        val testCompile = findByName("testCompile") ?: return@apply

        val testOutput by creating {
            extendsFrom(testCompile)
        }
        val boot by creating {
        }
    }

    val jvmTest: KotlinJvmTest = tasks.getByName<KotlinJvmTest>("jvmTest") {
        ignoreFailures = true
        maxHeapSize = "2g"
        exclude("**/*StressTest *")
    }

    tasks.create<Test>("stressTest") {
        classpath = files(jvmTest.classpath)
        testClassesDirs = files(jvmTest.testClassesDirs)

        ignoreFailures = true
        maxHeapSize = "2g"
        setForkEvery(1)
        systemProperty("enable.stress.tests", "true")
        include("**/*StressTest*")
    }

    tasks.getByName<Jar>("jvmJar").apply {
        manifest {
            attributes(
                "Implementation-Title" to name,
                "Implementation-Version" to configuredVersion
            )
        }
    }
}
