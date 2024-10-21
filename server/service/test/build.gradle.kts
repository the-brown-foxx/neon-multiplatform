plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlinx.serialization)
}

dependencies {
    implementation(libs.kotlin.test)
    implementation(libs.kotlinx.coroutines.test)
    implementation(libs.kotlinx.datetime)
    implementation(projects.common)
    implementation(projects.common.must)
    implementation(projects.common.hash)
    implementation(projects.server.service)
}