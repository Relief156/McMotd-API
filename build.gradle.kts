import org.apache.tools.ant.filters.ReplaceTokens

plugins {
    val kotlinVersion = "1.8.21"
    kotlin("jvm") version kotlinVersion
    kotlin("plugin.serialization") version kotlinVersion

    id("net.mamoe.mirai-console") version "2.16.0"
    // 添加shadowJar插件，使用兼容旧版Gradle的版本
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

group = "org.zrnq"
version = "1.2.3"
val ktor_version = "2.3.12"

repositories {
    mavenCentral()
}

/* Note: clean resources folder to update version number.
 * Or gradle will consider ProcessResources to be UP-TO-DATE.
 * https://github.com/gradle/gradle/issues/861
 */
tasks.withType(ProcessResources::class) {
    filter(ReplaceTokens::class, "tokens" to hashMapOf("version" to version))
}

dependencies {
    implementation(kotlin("reflect"))
    implementation("com.charleskorn.kaml:kaml:0.54.0")
    implementation("com.alibaba:fastjson:1.2.83")
    implementation("dnsjava:dnsjava:3.5.0")
    implementation("io.ktor:ktor-server-core:$ktor_version")
    implementation("io.ktor:ktor-server-netty:$ktor_version")
    implementation("org.gnu.inet:libidn:1.15")
}

tasks.create("CopyToLib", Copy::class) {
    into("${buildDir}/output/libs")
    from(configurations.runtimeClasspath)
}

// 配置shadowJar任务来生成包含所有依赖的可执行jar文件
tasks.shadowJar {
    archiveClassifier.set("all")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    
    // 设置mainClassName，用于独立运行
    manifest {
        attributes(
            "Main-Class" to "org.zrnq.mcmotd.StandaloneMainKt"
        )
    }
}

// 创建一个新任务，生成符合README要求的包含完整依赖的可执行jar文件
tasks.create<Jar>("buildStandaloneJar") {
    group = "build"
    description = "Builds a standalone JAR with all dependencies for independent execution"
    
    // 设置输出文件名格式为 mcmotd-x.x.x.mirai.jar，符合README要求
    archiveFileName.set("mcmotd-${version}.mirai.jar")
    destinationDirectory.set(file("${buildDir}/mirai"))
    
    // 包含所有运行时依赖
    from(sourceSets.main.get().output)
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
    
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    
    // 设置mainClassName，用于独立运行
    manifest {
        attributes(
            "Main-Class" to "org.zrnq.mcmotd.StandaloneMainKt"
        )
    }
    
    dependsOn(tasks.shadowJar)
}