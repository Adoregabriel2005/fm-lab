package br.local.fmlab;

/** Preserve a trailing stereo sample across partial AudioRecord reads. */
final class PcmFrameBuffer {
  private final int channels;
  private boolean pending;
  private short trailing;

  PcmFrameBuffer(int channels) {
    if (channels != 1 && channels != 2) throw new IllegalArgumentException("Channel count");
    this.channels = channels;
  }

  int prepare(short[] buffer) {
    if (pending) buffer[0] = trailing;
    return pending ? 1 : 0;
  }

  int complete(short[] buffer, int read) {
    int total = read + (pending ? 1 : 0);
    if (read < 0 || total > buffer.length) throw new IllegalArgumentException("PCM read size");
    pending = channels == 2 && (total % 2 != 0);
    if (pending) trailing = buffer[--total];
    return total;
  }
}
