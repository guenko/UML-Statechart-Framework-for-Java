package statechart;

import java.util.concurrent.ThreadFactory;
import org.apache.log4j.Logger;

/**
 * Used to create threads within the statechart framework. Allows the setting of
 * some thread attributes, e.g. thread group, thread name, daemon/user thread.
 */
public class StatechartThreadFactory implements ThreadFactory {

  private static Logger log = Logger.getLogger(StatechartThreadFactory.class);

  /** default for the deamonThread setting */
  private static boolean makeDaemonThreadsGlobal = false;

  /** the thread group of the threads */
  private ThreadGroup threadGroup;
  /** name of the group, is also a part of the thread name */
  private String groupName;
  /* whether to start the threads as daemon threads */
  private boolean makeDaemonThreads = false;

  /**
   * Whether to start the threads as daemon threads. The Java Virtual Machine
   * exits when the only threads running are all daemon threads, see
   * {@link java.lang.Thread#setDaemon}
   */
  public static void setMakeDaemonThreadsGlobal(boolean makeDaemonThreadsGlobal) {
    StatechartThreadFactory.makeDaemonThreadsGlobal = makeDaemonThreadsGlobal;
  }

  /**
   * Create ThreadFactory.
   * 
   * @param groupName name of thread group, also the base name for the threads
   * @param makeDaemonThreads if true, marks the created thread as a daemon
   * @see java.lang.Thread#setDaemon
   */
  public StatechartThreadFactory(String groupName, boolean makeDaemonThreads) {
    this.groupName = groupName;
    threadGroup = new ThreadGroup(groupName);
    this.makeDaemonThreads = makeDaemonThreads;
  }

  /**
   * Create ThreadFactory.
   * 
   * @param groupName name of thread group, also the base name for the threads
   */
  public StatechartThreadFactory(String groupName) {
    this.groupName = groupName;
    threadGroup = new ThreadGroup(groupName);
    this.makeDaemonThreads = makeDaemonThreadsGlobal;
  }

  /**
   * Creates a new thread with certain settings. Called by the ExecutorService
   * this factory is handed over when a new thread is needed.
   * 
   * @see java.util.concurrent.ExecutorService
   */
  @Override
  public Thread newThread(Runnable runnable) {
    Thread thread = new Thread(threadGroup, runnable);
    thread.setName(groupName + "-" + String.format("%02d", thread.getId()));
    thread.setDaemon(makeDaemonThreads);
    log.info("thread created, id: " + String.format("%08d", thread.getId()) + " name: " + thread.getName());
    return thread;
  }
}
