package com.twosigma.beaker.scala.util;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;

//import scala.tools.nsc.interpreter.IMain;
import com.twosigma.beaker.NamespaceClient;
import com.twosigma.beaker.jvm.object.SimpleEvaluationObject;

public class ScalaEvaluator {
  protected final String shellId;
  protected final String sessionId;
  protected List<String> classPath;
  protected List<String> imports;
  protected String outDir;
  //protected GroovyClasspathScanner cps;
  protected boolean exit;
  protected boolean updateLoader;
  protected final ThreadGroup myThreadGroup;
  protected workerThread myWorker;
  //protected GroovyAutocomplete gac;

  protected class jobDescriptor {
    String codeToBeExecuted;
    SimpleEvaluationObject outputObject;

    jobDescriptor(String c , SimpleEvaluationObject o) {
      codeToBeExecuted = c;
      outputObject = o;
    }
  }

  protected final Semaphore syncObject = new Semaphore(0, true);
  protected final ConcurrentLinkedQueue<jobDescriptor> jobQueue = new ConcurrentLinkedQueue<jobDescriptor>();

  public ScalaEvaluator(String id, String sId) {
    shellId = id;
    sessionId = sId;
    //cps = new GroovyClasspathScanner();
    //gac = createGroovyAutocomplete(cps);
    classPath = new ArrayList<String>();
    imports = new ArrayList<String>();
    exit = false;
    updateLoader = false;
    myThreadGroup = new ThreadGroup("tg"+shellId);
    startWorker();
  }

  protected void startWorker() {
    myWorker = new workerThread(myThreadGroup);
    myWorker.start();
  }

//  protected GroovyAutocomplete createGroovyAutocomplete(GroovyClasspathScanner c)
//  {
//	return new GroovyAutocomplete(c);
//  }

  public String getShellId() { return shellId; }

  public void killAllThreads() {
    cancelExecution();
  }

  public void cancelExecution() {
    myThreadGroup.interrupt();
    try {
      Thread.sleep(100);
    } catch (InterruptedException e) { }
    myThreadGroup.interrupt();
  }

  public void resetEnvironment() {
    cancelExecution();

    String cpp = "";
    for(String pt : classPath) {
      cpp += pt;
      cpp += File.pathSeparator;
    }
    cpp += File.pathSeparator;
    cpp += System.getProperty("java.class.path");
//    cps = new GroovyClasspathScanner(cpp);
//    gac = createGroovyAutocomplete(cps);
//    
//    for(String st : imports)
//      gac.addImport(st);

    updateLoader=true;
    syncObject.release();
  } 

  public void exit() {
    exit = true;
    cancelExecution();
  }

  public void setShellOptions(String cp, String in, String od) throws IOException {
    if(cp.isEmpty())
      classPath.clear();
    else
      classPath = Arrays.asList(cp.split("[\\s]+"));
    if (in.isEmpty())
      imports.clear();
    else
      imports = Arrays.asList(in.split("\\s+"));
    outDir = od;
    if(outDir!=null && !outDir.isEmpty()) {
      outDir = outDir.replace("$BEAKERDIR",System.getenv("beaker_tmp_dir"));
      try { (new File(outDir)).mkdirs(); } catch (Exception e) { }
    }
    resetEnvironment();
  }
  
  public void evaluate(SimpleEvaluationObject seo, String code) {
    // send job to thread
    jobQueue.add(new jobDescriptor(code,seo));
    syncObject.release();
  }

//  public List<String> autocomplete(String code, int caretPosition) {    
//    return gac.doAutocomplete(code, caretPosition,loader!=null ? loader.getLoader() : null);
//  }

//  protected ScalaDynamicClassLoader loader = null;
//  protected GroovyShell shell;

  protected class workerThread extends Thread {

    public workerThread(ThreadGroup tg) {
      super(tg, "worker");
    }
    
    /*
     * This thread performs all the evaluation
     */
    
    public void run() {
      jobDescriptor j = null;
      NamespaceClient nc = null;
      
      while(!exit) {
        try {
          // wait for work
          syncObject.acquire();
          
          // check if we must create or update class loader
          if(updateLoader) {
//            shell = null;
          }
          
          // get next job descriptor
          j = jobQueue.poll();
          if(j==null)
            continue;

//          if (shell==null) {
//            updateLoader=false;
//            newEvaluator();
//          }
        
//          if(loader!=null)
//            loader.clearCache();

          j.outputObject.started();

          nc = NamespaceClient.getBeaker(sessionId);
          nc.setOutputObj(j.outputObject);
          
//          Object result;
//          result = shell.evaluate(j.codeToBeExecuted);
//          j.outputObject.finished(result);
        } catch(Throwable e) {
          if(j!=null && j.outputObject != null) {
            if (e instanceof InterruptedException || e instanceof InvocationTargetException) {
              j.outputObject.error("... cancelled!");
            } else {
              j.outputObject.error(e.getMessage());
            }          
          }
        } finally {
          if(nc!=null) {
            nc.setOutputObj(null);
            nc = null;
          }
        }
      }
    }
      
    protected ClassLoader newClassLoader() throws MalformedURLException
    {
      URL[] urls = {};
      if (!classPath.isEmpty()) {
        urls = new URL[classPath.size()];
        for (int i = 0; i < classPath.size(); i++) {
          urls[i] = new URL("file://" + classPath.get(i));
        }
      }
//      loader = null;
//      ClassLoader cl;
//
//      loader = new ScalaDynamicClassLoader(outDir);
//      loader.addAll(Arrays.asList(urls));
//      cl = loader.getLoader();
//      return cl;
      return null;
    }

    protected void newEvaluator() throws MalformedURLException
    {
//      ImportCustomizer icz = new ImportCustomizer();
//
//      if (!imports.isEmpty()) {
//        for (int i = 0; i < imports.size(); i++) {
//          // should trim too
//          if (imports.get(i).endsWith(".*")) {
//            icz.addStarImports(imports.get(i).substring(0, imports.get(i).length() - 2));
//          } else {
//            icz.addImports(imports.get(i));
//          }
//        }
//      }
//      CompilerConfiguration config = new CompilerConfiguration().addCompilationCustomizers(icz);
//      shell = new GroovyShell(newClassLoader(), new Binding(), config);
    }
  }

}

