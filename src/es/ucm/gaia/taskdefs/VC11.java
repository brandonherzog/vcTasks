//WARNING UNTESTED!!!
package es.ucm.gaia.taskdefs;

import org.apache.tools.ant.types.*;
import org.apache.tools.ant.*;

//----------------------------------------
public class VC11 extends CompileTask
  {
  public VC11()
  {
     cFailureMessage = "*** VC11 failed ***";
     cProjectFileExtension = ".sln";
     cProjectRootFolderPropertyName = "VC11_ROOT";

     cLogFilePropertyName = "VC11LOGFILE";
     cDefaultLogFile = "logfile.txt";

     cCompilerFolderPropertyName = "VC11COMPILERFOLDER";
     cCompilerExe = "devenv.com";
     cBuildModePropertyName = "VC11BUILDMODE";
  }

  //----------------------------------------
  //-- create the command line
  protected Commandline buildCommandLine(String theFullpath)
  {
      Commandline commandLine = new Commandline();
      commandLine.setExecutable(myCompilerPath);

      //the output log file for any error messages
      commandLine.createArgument().setLine("/out " + myLogFile);

      if (isUpgrade)
	  commandLine.createArgument().setLine("/upgrade");
      else {
          if (isClean)
             commandLine.createArgument().setLine("/clean ");
          else if (isRebuild)
             commandLine.createArgument().setLine("/rebuild ");
          else
            commandLine.createArgument().setLine("/build ");

          //switch indicating which project to build
          commandLine.createArgument().setLine(myBuildMode + " ");
      }

      //the fully qualified path to the project file
      Path p = new Path(getProject());
      p.setPath(theFullpath);
      commandLine.createArgument().setPath(p);

      return commandLine;
  }

  //----------------------------------------
  //-- intenta encontrar el lugar donde está el compilador
  //-- utilizando la variable de entorno correspondiente
  protected String guessCompilerFolder() {
    String vcCommon = System.getenv("VS110COMNTOOLS");
    if ((vcCommon == null) || (vcCommon == "")) {
	log("VS110COMNTOOLS not set. Task rely on the PATH for finding Visual Studio tools (and we assume the complete edition, not the Express one).", Project.MSG_WARN);
	return "";
    }

    return vcCommon + "../IDE";
  }

}

