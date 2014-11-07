package com.twosigma.beaker.groovy.utils;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;

import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.customizers.ImportCustomizer;

import com.twosigma.beaker.autocomplete.ClasspathScanner;
import com.twosigma.beaker.jvm.classloader.DynamicClassLoader;
import com.twosigma.beaker.jvm.object.SimpleEvaluationObject;

public class GroovyEvaluator {
  private final String shellId;
  private List<String> classPath;
  private List<String> imports;
  private String outDir;
  private ClasspathScanner cps;
  private GroovyShell shell;

  private boolean exit;
  private boolean updateLoader;
  private final ThreadGroup myThreadGroup;
  private final workerThread myWorker;

  private class jobDescriptor {
    String codeToBeExecuted;
    SimpleEvaluationObject outputObject;

    jobDescriptor(String c , SimpleEvaluationObject o) {
      codeToBeExecuted = c;
      outputObject = o;
    }
  }

  private final Semaphore syncObject = new Semaphore(0, true);
  private final ConcurrentLinkedQueue<jobDescriptor> jobQueue = new ConcurrentLinkedQueue<jobDescriptor>();

  public GroovyEvaluator(String id) {
    shellId = id;
    cps = new ClasspathScanner();
    classPath = new ArrayList<String>();
    imports = new ArrayList<String>();
    exit = false;
    updateLoader = false;
    myThreadGroup = new ThreadGroup("tg"+shellId);
    myWorker = new workerThread(myThreadGroup);
    myWorker.start();
  }

  public String getShellId() { return shellId; }

  public void killAllThreads() {
    cancelExecution();
  }

  public void cancelExecution() {
    myThreadGroup.interrupt();
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
      try { (new File(outDir)).mkdirs(); } catch (Exception e) { }
    }

    String cpp = "";
    for(String pt : classPath) {
      cpp += pt;
      cpp += File.pathSeparator;
    }
    cpp += File.pathSeparator;
    cpp += System.getProperty("java.class.path");
    cps = new ClasspathScanner(cpp);
  }
  
  public void evaluate(SimpleEvaluationObject seo, String code) {
    // send job to thread
    jobQueue.add(new jobDescriptor(code,seo));
    syncObject.release();
  }

  public List<String> autocomplete(String code, int caretPosition) {
    return null;
  }

  private class workerThread extends Thread {
    private DynamicClassLoader loader = null;

    public workerThread(ThreadGroup tg) {
      super(tg, "worker");
    }
    
    /*
     * This thread performs all the evaluation
     */
    
    public void run() {
      jobDescriptor j = null;
      
      while(!exit) {
        try {
          // wait for work
          syncObject.acquire();
          
          // check if we must create or update class loader
          if(loader==null || updateLoader) {
            shell = null;
          }
          
          // get next job descriptor
          j = jobQueue.poll();
          if(j==null)
            continue;

          if (shell==null) newEvaluator();
        
          if(loader!=null)
            loader.clearCache();

          j.outputObject.started();
          Object result;
          result = shell.evaluate(j.codeToBeExecuted);
          j.outputObject.finished(result);
        } catch(Exception e) {
          if(j!=null && j.outputObject != null) {
            if (e instanceof InterruptedException || e instanceof InvocationTargetException) {
              j.outputObject.error("... cancelled!");
            } else {
              j.outputObject.error(e.getMessage());
            }          
          }
        }
      }
    }
      
    private void newEvaluator() throws MalformedURLException
    {
      URL[] urls = {};
      if (!classPath.isEmpty()) {
        urls = new URL[classPath.size()];
        for (int i = 0; i < classPath.size(); i++) {
          System.out.println("url is '"+classPath.get(i)+"'");
          urls[i] = new URL("file://" + classPath.get(i));
        }
      }
      ImportCustomizer icz = new ImportCustomizer();

      if (!imports.isEmpty()) {
        for (int i = 0; i < imports.size(); i++) {
          // should trim too
          if (imports.get(i).endsWith(".*")) {
            icz.addStarImports(imports.get(i).substring(0, imports.get(i).length() - 2));
          } else {
            icz.addImports(imports.get(i));
          }
        }
      }
      CompilerConfiguration config = new CompilerConfiguration().addCompilationCustomizers(icz);
      loader = null;
      ClassLoader cl;
      if(outDir!=null && !outDir.isEmpty()) {
        DynamicClassLoader dl = new DynamicClassLoader(outDir);
        dl.addAll(Arrays.asList(urls));
        cl = dl.getLoader();
      } else
        cl = new URLClassLoader(urls);
      shell = new GroovyShell(cl, new Binding(), config);
    }
  } 
}

