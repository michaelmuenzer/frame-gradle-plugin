package com.mounacheikhna.screenshots

import com.android.build.gradle.AppPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.StopExecutionException

/**
 * Created by m.cheikhna on 31/12/2015.
 */
class ScreenshotsPlugin implements Plugin<Project> {

  private static final String TASK_PREFIX = "screenshots"
  private static final String GROUP_SCREENSHOTS = "screenshots"

  @Override
  void apply(Project project) {
    if (!hasPlugin(project, AppPlugin)) {
      throw new StopExecutionException("The 'com.android.application' plugin is required.")
    }
    project.extensions.add("screenshots", ScreenshotsExtension)

    Task allScreenshotsTask = project.tasks.create("all$TASK_PREFIX")
    allScreenshotsTask.group = GROUP_SCREENSHOTS
    allScreenshotsTask.description =
        '''Takes screenshots generated by spoon on all the connected devices for variation,
            copies them into play folder each in the right place.'''


    project.afterEvaluate {

      Task spoonTasks = project.tasks.create("allSpoonTask")
      List<String> locales = project.screenshots.locales
      String productFlavor = project.screenshots.productFlavor
      def flavorTaskName = productFlavor.capitalize()
      spoonTasks.dependsOn project.tasks.findByName("assemble$flavorTaskName")
      spoonTasks.dependsOn project.tasks.findByName("assembleAndroidTest")

      locales.each {
        Task spoonLocalTask = createSpoonRunTaskByLocale(project, it)
        spoonTasks.dependsOn spoonLocalTask
      }

      Task processTask = project.tasks.create("$it$TASK_PREFIX", ProcessSpoonOutputTask)
      Task convertImagesTask = createImageMagicAllTask(project)

      convertImagesTask.dependsOn spoonTasks
      processTask.dependsOn convertImagesTask

      allScreenshotsTask.dependsOn processTask
    }
  }

  private Task createSpoonRunTaskByLocale(Project project, String locale) {
    String productFlavor = project.screenshots.productFlavor
    String prefixApk = "${project.buildDir}/outputs/apk/app-$productFlavor-${project.screenshots.buildType}"
    String apkPath = "$prefixApk-unaligned.apk"
    //TODO: maybe replace -unaligned part with regex match like **
    String testApkPath = "$prefixApk-androidTest-unaligned.apk"
    String spoonRunnerLibPath = "${project.rootDir}/" +
        "" + "screenshots-plugin/lib/spoon-runner-1.3.1-jar-with-d" + "ependencies.jar"
    Task task = project.tasks.create("${locale}spoonRunTask", Exec) {
      commandLine "java", "-jar", "$spoonRunnerLibPath",
          "--apk", "$apkPath", "--test-apk", "$testApkPath",
          "--class-name", "com.mounacheikhna.screenshots.ScreenshotsTest",
          "--e", "locale=$locale"
    }
    task
  }

  private Task createImageMagicAllTask(Project project) {
    String buildDestDir = project.screenshots.buildDestDir ?: "spoon-output"
    //String imagesParentFolder = "$buildDestDir/${project.screenshots.buildType}/image/"
    String imagesParentFolder = "${project.projectDir}/$buildDestDir/image/"

    String frameFileName = "${project.projectDir}/frames/galaxy_nexus_port_back.png";

    String deviceFrameRequiredSize = "1270x1290"
    String labelTextSize = "40"
    String topOffset = "40"
    String screenshotsTitle = "Title for this screenshot"

    Task imageMagicAll = project.tasks.create("imageMagicAll") {

      doLast {
        //TODO: some parameters should be provided by user such as background color, text label, frame file path
        new File(imagesParentFolder).listFiles({ it.isDirectory() } as FileFilter)
            .each {
          dir ->
            /*dir.traverse(
                type: groovy.io.FileType.FILES,
                nameFilter: ~/.*\.png/
            ) {*/
            dir.eachFileRecurse {
              if (it.isFile() && it.name.contains(".png")) {

                String imageFileName = it.name
                String taskSuffixName = "${dir.name}$imageFileName"
                String imageDir = it.parent

                project.tasks.create("c1$taskSuffixName", Exec) {
                  workingDir imageDir
                  commandLine "convert", "$imageFileName", "-resize", deviceFrameRequiredSize,
                      "$imageFileName"
                }.execute()

                project.tasks.create("c2$taskSuffixName", Exec) {
                  workingDir imageDir
                  commandLine "convert", "$frameFileName", "$imageFileName",
                      "-gravity", "center", "-compose", "over", "-composite",
                      "-fill", "gold", "-channel", "RGBA", "-opaque", "none", "$imageFileName"
                }.execute()

                project.tasks.create("c3$taskSuffixName", Exec) {
                  workingDir imageDir
                  commandLine "convert", "$imageFileName", "-background", "Gold", "-gravity",
                      "North",
                      "-pointsize", "$labelTextSize", "-density", "100", "-fill", "white",
                      "-annotate", "+0+$topOffset", "$screenshotsTitle",
                      "$imageFileName"
                }.execute()
              }
            }
        }
      }
    }

    imageMagicAll.group = GROUP_SCREENSHOTS
    imageMagicAll
  }

  static def hasPlugin(Project project, Class<? extends Plugin> plugin) {
    return project.plugins.hasPlugin(plugin)
  }
}
