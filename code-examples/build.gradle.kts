import book.SourceRoots
import book.processFile
import book.processFiles
import java.lang.System.getProperty

plugins {
    kotlin("jvm")
}

dependencies {
    implementation(kotlin("reflect"))
    implementation("com.natpryce:result4k:2.0.0")
}

repositories {
    mavenCentral()
}

