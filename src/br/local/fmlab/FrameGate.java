package br.local.fmlab;

/**
 * At most one queued audio frame per capture generation. A delayed callback from a stopped session
 * cannot unlock a newer session.
 */
final class FrameGate {
  private int generation;
  private boolean pending;

  synchronized void reset(int value) {
    generation = value;
    pending = false;
  }

  synchronized boolean acquire(int value) {
    if (value != generation || pending) return false;
    pending = true;
    return true;
  }

  synchronized void release(int value) {
    if (value == generation) pending = false;
  }
}
