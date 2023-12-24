import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
group = "raylras.zen.binding"

plugins {
    kotlin("jvm") version "1.9.21"
}

dependencies {
    implementation("com.github.jnr:jnr-ffi:2.2.15")
    implementation("net.java.dev.jna:jna:5.14.0")
}


tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = "1.8"
    }
}


tasks.named<Copy>("distDeps") {

}
