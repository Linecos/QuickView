import dev.kikugie.stonecutter.data.ParsedVersion
import net.fabricmc.loom.api.LoomGradleExtensionAPI
import org.gradle.api.tasks.SourceSetContainer

fun getProperty(name: String) =
    providers.gradleProperty(name).orNull
        ?: project.findProperty(name)?.toString()
        ?: ""

val modId = getProperty("mod_id")
val mavenGroup = getProperty("maven_group")
val minecraftVersion = getProperty("minecraft_version")
val modVersion = getProperty("mod_version")
val minecraftRequirementVersion = getProperty("minecraft_requirement_version")
val loaderRequirementVersion = getProperty("loader_requirement_version")
val archivesBaseName = getProperty("archives_base_name")
val libguiVersion = getProperty("libgui_version")
val libguiRequirementVersion = getProperty("libgui_requirement_version")
val fabricApiVersion = getProperty("fabric_api_version")
val fabricApiRequirementVersion = getProperty("fabric_api_requirement_version")
val javaRequirementVersion = getProperty("java_requirement_version")

val unobfuscated = ParsedVersion(minecraftVersion) >= ParsedVersion("26.1")
apply(plugin = if (unobfuscated) "net.fabricmc.fabric-loom" else "net.fabricmc.fabric-loom-remap")
val loomExtension = extensions.getByType(LoomGradleExtensionAPI::class)

val requiredJava: JavaVersion = if (unobfuscated) JavaVersion.VERSION_25 else JavaVersion.VERSION_21

val sourceSets = extensions.getByType(SourceSetContainer::class.java)
sourceSets["main"].java.setSrcDirs(listOf(layout.buildDirectory.dir("generated/stonecutter/main/java")))
sourceSets["test"].java.setSrcDirs(listOf(layout.buildDirectory.dir("generated/stonecutter/test/java")))
tasks.named("compileJava") { dependsOn("stonecutterGenerate") }
tasks.named("compileTestJava") { dependsOn("stonecutterGenerateTest") }

val gitBranch = try {
    providers.exec {
        commandLine("git", "rev-parse", "--abbrev-ref", "HEAD")
    }.standardOutput.asText.get().trim()
} catch (e: Exception) {
    ""
}
val isDevBuild = gitBranch == "dev" && !project.hasProperty("release")
val fullModVersion = modVersion + if (isDevBuild) "-dev" else ""

group = mavenGroup
version = "$fullModVersion-mc$minecraftVersion"
base.archivesName = archivesBaseName

repositories {
    maven {
        name = "AliyunPublic"
        url = uri("https://maven.aliyun.com/repository/public")
    }
    maven {
        name = "Fabric"
        url = uri("https://maven.fabricmc.net/")
    }
    maven {
        name = "AlexIIL"
        url = uri("https://staging.alexiil.uk/maven/")
    }
    mavenCentral()
}

dependencies {
    fun autoImplementation(dep: Any) =
        add(if (unobfuscated) "implementation" else "modImplementation", dep)

    "minecraft"("com.mojang:minecraft:$minecraftVersion")
    if (!unobfuscated) {
        "mappings"(loomExtension.layered { officialMojangMappings() })
    }

    autoImplementation("net.fabricmc:fabric-loader:${getProperty("loader_version")}")
    autoImplementation("net.fabricmc.fabric-api:fabric-api:$fabricApiVersion")
    autoImplementation("io.github.cottonmc:LibGui:$libguiVersion")

    implementation("com.github.houbb:pinyin:${getProperty("pinyin_version")}")
    add("include", "com.github.houbb:pinyin:${getProperty("pinyin_version")}")
    add("include", "com.github.houbb:heaven:${getProperty("pinyin_heaven_version")}")
    add("include", "com.github.houbb:nlp-common:${getProperty("pinyin_nlp_common_version")}")

    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

val compatibilityLevel = "JAVA_${requiredJava.majorVersion}"
val resourceProps = mapOf(
    "version" to fullModVersion,
    "mc" to minecraftRequirementVersion,
    "loader" to loaderRequirementVersion,
    "libgui" to libguiRequirementVersion,
    "fabric_api" to fabricApiRequirementVersion,
    "java" to javaRequirementVersion,
    "compatibility_level" to compatibilityLevel
)

tasks.processResources {
    inputs.properties(resourceProps)
    filesMatching("fabric.mod.json") {
        expand(resourceProps)
    }
    filesMatching("*.mixins.json") {
        expand("compatibility_level" to compatibilityLevel)
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(requiredJava.majorVersion.toInt())
    options.encoding = "UTF-8"
}

java {
    sourceCompatibility = requiredJava
    targetCompatibility = requiredJava
}

tasks.test {
    useJUnitPlatform()
}


if (unobfuscated) {
    stonecutter {
        replacements.string(true) {
            replace("GuiGraphics", "GuiGraphicsExtractor")
            replace("KeyBindingHelper", "KeyMappingHelper")
            replace("client.keybinding.v1", "client.keymapping.v1")
            replace("registerKeyBinding", "registerKeyMapping")
            replace("context.drawString", "context.text")
        }
    }
}

if (ParsedVersion(minecraftVersion) >= ParsedVersion("26.2")) {
    stonecutter {
        replacements.string(true) {
            replace("setScreen", "setScreenAndShow")
            replace("Minecraft.getInstance().screen", "Minecraft.getInstance().gui.screen()")
            replace("renderHandsWithItems", "submitHandsWithItems")
        }
    }
}

if (ParsedVersion(minecraftVersion) < ParsedVersion("1.21.11")) {
    stonecutter {
        replacements.string(true) {
            replace("net.minecraft.resources.Identifier", "net.minecraft.resources.ResourceLocation")
            replace("Identifier", "ResourceLocation")
            replace("net.minecraft.util.Util", "net.minecraft.Util")
            replace(".identifier()", ".location()")
        }
    }
}

if (ParsedVersion(minecraftVersion) >= ParsedVersion("26.3")) {
    stonecutter {
        replacements.string(true) {
            replace("InputConstants.Type.KEYSYM", "InputConstants.Type.KEYBOARD")
            replace("GLFW.GLFW_KEY_UNKNOWN", "InputConstants.UNKNOWN.getValue()")
            replace("GLFW.GLFW_KEY_ESCAPE", "InputConstants.KEY_ESCAPE")
            replace("GLFW.GLFW_KEY_V", "InputConstants.KEY_V")
            replace("GLFW.GLFW_KEY_B", "InputConstants.KEY_B")
            replace("GLFW.GLFW_KEY_N", "InputConstants.KEY_N")
            replace("GLFW.GLFW_KEY_G", "InputConstants.KEY_G")
            replace("GLFW.GLFW_KEY_H", "InputConstants.KEY_H")
            replace("import org.lwjgl.glfw.GLFW;", "// 26.3 uses SDL input; GLFW constants are replaced with InputConstants.")
        }
    }
}
