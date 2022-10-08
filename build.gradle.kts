import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.7.20"
    `java-gradle-plugin`
}

group = "me.yuugiri.fluiditygradle"
version = "1.0-SNAPSHOT"

repositories {
    gradlePluginPortal()
    mavenCentral()
}

dependencies {
    compileOnly(gradleApi())
    implementation(kotlin("stdlib"))
    implementation("org.ow2.asm:asm-tree:9.4")
    implementation("com.google.code.gson:gson:2.2.4")
    implementation("net.md-5:SpecialSource:1.11.0")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

gradlePlugin {
    plugins {
        create("FluidityGradle") {
            id = "me.yuugiri.fluiditygradle"
            implementationClass = "me.yuugiri.fluiditygradle.FluidityGradlePlugin"
        }
    }
}