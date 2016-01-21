package com.mounacheikhna.screenshots

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.TaskAction

/**
 * Created by m.cheikhna on 15/01/2015.
 */
public class FrameTask extends DefaultTask {

    private static final String GROUP_SCREENSHOTS = "screenshots"

    @TaskAction
    void performTask() {
        String buildDestDir = project.screenshots.buildDestDir ?: "spoon-output"
        //String imagesParentFolder = "$buildDestDir/${project.screenshots.buildType}/image/"
        String imagesParentFolder = "${project.projectDir}/$buildDestDir/image/"

        //TODO: make getting this frame more dynamic so that users can use the frame they want.
        String frameFileName = "${project.projectDir}/frames/galaxy_nexus_port_back.png";

        String deviceFrameRequiredSize = "1270x1290"
        String labelTextSize = "40"
        String topOffset = "40"
        String screenshotsTitle = "Title for this screenshot"


            //doLast {
                //TODO: some parameters should be provided by user such as background color, text label, frame file path
                new File(imagesParentFolder).listFiles({ it.isDirectory() } as FileFilter)
                        .each {
                    dir ->
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

        /*imageMagicAll.group = GROUP_SCREENSHOTS
        imageMagicAll*/
    }


}
