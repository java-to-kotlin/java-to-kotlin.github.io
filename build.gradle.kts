import book.SourceRoots
import book.processFile
import book.processFiles
import java.lang.System.getProperty


plugins {
    kotlin("jvm") version "1.5.20"
}

dependencies {
    implementation(kotlin("reflect"))
    implementation("com.natpryce:result4k:2.0.0")
}

repositories {
    mavenCentral()
}

tasks {
    val retagCode = register<Exec>("retagCode") {
        workingDir(rootDir)
        commandLine("../refactoring-to-kotlin-book/retag-worked-example")
    }
    
    val sourceRoots = SourceRoots(
        workedExample = rootDir.resolve("../refactoring-to-kotlin-code"),
        digressionCode = rootDir
    )
    
    named("build") {
        dependsOn(retagCode)
        
        doLast {
            processFiles(
                inputRoot = projectDir,
                outputRoot = projectDir,
                sourceRoots = sourceRoots,
                abortOnFailure = true,
                kotlinVersion = rootDir.resolve(".kotlin-version").readText().trim()
            )
        }
    }
    
    // Use ./gradlew expand-one -Dsingle-file=$FilePath$
    register("expand-one") {
        dependsOn(retagCode)
        
        doLast {
            val file = getProperty("single-file")?.let {
                File(it).takeIf { it.isFile }
            } ?: error("File not specified or found in property single-file")
            processFile(
                src = file,
                dest = file,
                roots = sourceRoots,
                abortOnFailure = true,
                kotlinVersion = rootDir.resolve(".kotlin-version").readText().trim()
            )
        }
    }
}

