/**
 Tareas de Ant para compilar proyectos de
 Visual Studio, pudiendo seleccionar la versión
 concreta de Visual a utilizar.

 Realizadas mediante ligeras modificaciones de
 http://jtaskdefs.sourceforge.net/
 */

package es.ucm.gaia.taskdefs;

import java.io.*;
import org.apache.tools.ant.types.*;
import org.apache.tools.ant.taskdefs.*;
import org.apache.tools.ant.*;

public abstract class CompileTask extends Task
{
  private String myProjectName = null;
  private String myProjectRootFolder = null;
  private String myProjectFolder = null;
  private FileSet myFileset = null;
  private boolean isFailOnError = true;
  private String myCompilerFolder = null;

  protected String myLogFile = null;
  protected String myCompilerPath = null;
  protected String myBuildMode;
  protected boolean isRebuild = false;

    // Gestión del objetivo clean. Si se especifica rebuild="true", no
    // se puede indicar clean="true"
    protected boolean isClean = false;

    // Gestión del objetivo upgrade. Si se especifica upgrade="true", no
    // se compilará/limpiará el proyecto, por lo que ni rebuild ni
    // clean se podrán utilizar.
    protected boolean isUpgrade = false;

  //constants...
  protected String cFailureMessage;
  protected String cProjectFileExtension;
  protected String cProjectRootFolderPropertyName;
  protected String cBuildModePropertyName;

  protected String cLogFilePropertyName;
  protected String cDefaultLogFile;

  protected String cCompilerFolderPropertyName;
  protected String cCompilerExe;


  //----------------------------------------
  //-- called by ant via reflection
  public void execute() //throws BuildException
  {
    initialize();
    preCompile();
    if (myFileset == null)
      compile(deriveFullProjectFolder());
    else
      compileFromFileSet();
    postCompile();
  }

  //----------------------------------------
  //-- ATTRIBUTES
  //----------------------------------------

  //----------------------------------------
  public void setProject(String theProjectName)
  {
    myProjectName = theProjectName;
  }

  //----------------------------------------
  public void setBuildMode(String theBuildMode)
  {
    myBuildMode = theBuildMode;
  }

  //----------------------------------------
  public void setCompilerFolder(String theCompilerFolder)
  {
    myCompilerFolder = theCompilerFolder;
  }

  //----------------------------------------
  public void setProjectRootFolder(String theProjectRootFolder)
  {
    myProjectRootFolder = theProjectRootFolder;
  }

  //----------------------------------------
  public void setProjectFolder(String theProjectFolder)
  {
    myProjectFolder = theProjectFolder;
  }

  //----------------------------------------
  public void setFailonerror(boolean theFlag)
  {
    isFailOnError = theFlag;
  }

  //----------------------------------------
  public void setLogFile(String theLogFile)
  {
    myLogFile = theLogFile;
  }

  //----------------------------------------
  //-- a file set that specifies the project files
  //-- for doing multiple compiles
  public void addTargets(FileSet set)
  {
    if (myFileset != null) throw new BuildException("Only one fileset allowed");
    myFileset = set;
  }

  //----------------------------------------
  public void setRebuild(boolean theFlag)
  {
    if ((isClean == true) || (isUpgrade == true)) throw new BuildException("Clean and upgrade options are not compatible with rebuild.");
    isRebuild = theFlag;
  }

  //----------------------------------------
  public void setClean(boolean theFlag)
  {
    if ((isRebuild == true) || (isUpgrade == true)) throw new BuildException("Rebuild and upgrade options are not compatible with clean.");
    isClean = theFlag;
  }

  //----------------------------------------
  public void setUpgrade(boolean theFlag)
  {
    if ((isRebuild == true) || (isClean == true)) throw new BuildException("Clean and rebuild options are not compatible with upgrade.");
    isUpgrade = theFlag;
  }

  //----------------------------------------
  //-- protected from here on
  //----------------------------------------
  protected abstract Commandline buildCommandLine(String theFullpath);
  protected abstract String guessCompilerFolder();  
  protected void preCompile() {}
  protected void postCompile() {}

  //----------------------------------------
  //-- private from here on
  //----------------------------------------

  //----------------------------------------
  private void initialize()
  {
    //this next bit of code from a post to:
    //   http://marc.theaimsgroup.com/?l=ant-dev&m=109412751329874&w=2
    //someone else had a problem with 
    //    "Listener attempted to access System.out - infinite loop terminated"
    //it seems that the Ant class loader causes a System.out to be generated
    //when a BuildEvent is created. The Listener is trapping System.out output 
    //which causes an infinite loop.
    try 
    {
			// force class loading, so that no additional build-event
			// is triggered when the messageLogged()-method
			// is invoced:
			Class.forName( "org.apache.tools.ant.BuildEvent" );
		} 
		catch (ClassNotFoundException e) 
		{
			e.printStackTrace();
			throw new BuildException("Unable to load BuildEvent from the classpath.", e);
		}
		
    initializeProjectName();       //must come first!
    initializeProjectFolder();     //second
    initializeProjectRootFolder(); //third
    initializeLogFile();
    initializeCompilerFolder();
    initializeCompilerExecutable();
    initializeBuildMode();

    myCompilerPath = getExePath(cCompilerExe);
  }

  //----------------------------------------
  private String getExePath(String theExeFileName)
  {
    if (myCompilerFolder == null || myCompilerFolder == "") return theExeFileName;
    return  myCompilerFolder + "/" + theExeFileName;
  }

  //----------------------------------------
  private void initializeProjectName()
  {
    //if user has set it, we're done
    if (myProjectName != null) return;

    //none of the above, use the default
    myProjectName = getOwningTarget().toString();
  }

  //----------------------------------------
  private void initializeProjectFolder()
  {
    //if user has set it, we're done
    if (myProjectFolder != null) return;

    //none of the above, use the project name
    setProjectFolder(myProjectName);
  }

  //----------------------------------------
  private void initializeProjectRootFolder()
  {
    //if user has set it, we're done
    if (myProjectRootFolder != null) return;

    //if there's a property for it, create the dir from that
    if (getProject().getProperty(cProjectRootFolderPropertyName) != null)
    {
      setProjectRootFolder(getProject().getProperty(cProjectRootFolderPropertyName));
      return;
    }

    //none of the above, use the current directory
    setProjectRootFolder("./");
  }

  //----------------------------------------
  private void initializeLogFile()
  {
    //if user has set it, we're done
    if (myLogFile != null) return;

    //if there's a property for it, we're done
    myLogFile = getProject().getProperty(cLogFilePropertyName);
    if (myLogFile != null) return;

    //none of the above, use the default
    myLogFile = cDefaultLogFile;
  }

  //----------------------------------------
  private void initializeCompilerFolder()
  {
    //if user has set it, we're done
    if (myCompilerFolder != null) return;

    //if there's a property for it, create the dir from that
    if (getProject().getProperty(cCompilerFolderPropertyName) != null)
    {
      setCompilerFolder(getProject().getProperty(cCompilerFolderPropertyName));
      return;
    }

    // No ha sido establecido por el usuario; buscamos
    // en las variables de entorno donde puede estar...
    String guess;
    guess = guessCompilerFolder();

    if ((guess == null) || (guess.equals(""))) {
       setCompilerFolder("");
       return;
    }

    setCompilerFolder(guess);

    //    String vcCommon = System.getProperty("VS80COMNTOOLS");
  }

  private void initializeCompilerExecutable()
  {
      // Si no tenemos directorio, asumimos devenv.exe
      if (myCompilerFolder.equals("")) {
	  cCompilerExe = "devenv.exe";
	  return;
      }

      // Miramos si existe devenv.exe
      String devenv = myCompilerFolder + java.io.File.separator + "devenv.exe";
      String express = myCompilerFolder + java.io.File.separator + "vcexpress.exe";
      //log("Buscando " + devenv, Project.MSG_WARN);
      //log("y buscando " + express, Project.MSG_WARN);
      if ((new java.io.File(devenv)).exists()) {
	  cCompilerExe = "devenv.exe";
	  //log("visual encontrado", Project.MSG_WARN);
      } else if (new java.io.File(express).exists()) {
	  cCompilerExe = "vcexpress.exe";
      } else {
	log("Visual Studio executable not found. Is the environment variable correct?", Project.MSG_ERR);
      }
  }


  //----------------------------------------
  private void initializeBuildMode()
  {
    //if user has set it, we're done
    if (myBuildMode != null) return;

    //if there's a property for it, create the dir from that
    if (getProject().getProperty(cBuildModePropertyName) != null)
    {
      setBuildMode(getProject().getProperty(cBuildModePropertyName));
      return;
    }

    //none of the above, use the current directory
    setBuildMode("release");
  }


  //----------------------------------------
  //-- gather up all the project files and compile each one
  private void compileFromFileSet()
  {
    DirectoryScanner ds = myFileset.getDirectoryScanner(getProject());
    ds.scan();
    compile(ds.getBasedir(), ds.getIncludedFiles());
  }

  //----------------------------------------
  //-- builds the command line and runs it
  private void compile(String theFullpath)
  {
    Commandline commandLine = buildCommandLine(theFullpath);
    log("Command= " + commandLine.toString());

    try
    {
      runCommand(commandLine, theFullpath);
    }
    catch (IOException e)
    {
      throw new BuildException("failed: " + e, e, getLocation());
    }
  }

  //----------------------------------------
  private void compile(File baseDir, String[] files)
  {
    if (files == null) return;

    for (int i = 0; i < files.length; ++i)
    {
      compile(baseDir + "/" + files[i]);
    }
  }

  //----------------------------------------
  private String deriveFullProjectFolder()
  {
    return myProjectRootFolder + "/" + myProjectFolder + "/" + myProjectName + cProjectFileExtension;
  }

  //----------------------------------------
  //-- executes the given command line and reports any errors
  private void runCommand(Commandline commandLine, String theFullpath) throws IOException
  {
    if (executeCommand(commandLine) == 0) return;

    String errorMessage = cFailureMessage + " Target='" + theFullpath + "'";
    if (isFailOnError)
      throw new BuildException(errorMessage, getLocation());
    else
      log(errorMessage);
  }

  //----------------------------------------
  private int executeCommand(Commandline commandLine) throws IOException
  {
    Execute exe = new Execute();
    exe.setCommandline(commandLine.getCommandline());
    return exe.execute();
  }
}

