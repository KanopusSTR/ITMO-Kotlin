plugins {
    id("org.jlleitschuh.gradle.ktlint") version "11.5.0"
    id("io.gitlab.arturbosch.detekt") version "1.23.1"
}

kotlin {
    jvmToolchain(17)
}

configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
    debug.set(true)
}

repositories {
    mavenCentral()
}

detekt {
    toolVersion = "1.23.1"
    config.setFrom(file("../../detekt.yml"))
    buildUponDefaultConfig = true
}

dependencies {
    implementation(libs.ktlint.gradle)
    implementation(libs.kotlin.gradle.plugin)

    testImplementation(libs.bundles.testing)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
