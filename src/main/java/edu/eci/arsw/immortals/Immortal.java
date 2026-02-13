package edu.eci.arsw.immortals;

import edu.eci.arsw.concurrency.PauseController;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

public final class Immortal implements Runnable {
  private final String name;
  private int health;
  private final int damage;
  private final List<Immortal> population;
  private final ScoreBoard scoreBoard;
  private final PauseController controller;
  private volatile boolean running = true;

  public Immortal(String name, int health, int damage, List<Immortal> population, ScoreBoard scoreBoard, PauseController controller) {
    this.name = Objects.requireNonNull(name);
    this.health = health;
    this.damage = damage;
    this.population = Objects.requireNonNull(population);
    this.scoreBoard = Objects.requireNonNull(scoreBoard);
    this.controller = Objects.requireNonNull(controller);
  }

  public String name() { return name; }
  public synchronized int getHealth() { return health; }
  public boolean isAlive() { return getHealth() > 0 && running; }
  public void stop() { running = false; }

  @Override public void run() {
    try {
      while (running) {
        controller.awaitIfPaused();
        if (!running) break;
        var opponent = pickOpponent();
        if (opponent == null) continue;
        String mode = System.getProperty("fight", "ordered");
        if ("naive".equalsIgnoreCase(mode)) fightNaive(opponent);
        else fightOrdered(opponent);
        Thread.sleep(2);
      }
    } catch (InterruptedException ie) {
      Thread.currentThread().interrupt();
    }
  }

  private Immortal pickOpponent() {
    int size = population.size();
    if (size <= 1) return null;
    int attempts = 0;
    int maxAttempts = Math.min(10, size * 2);

    while (attempts < maxAttempts) {
      try {
        int index = ThreadLocalRandom.current().nextInt(size);
        Immortal other = population.get(index);

        if (other != this && other.isAlive()) {
          return other;
        }
      } catch (IndexOutOfBoundsException e) {
        size = population.size();
        if (size <= 1) return null;
      }
      attempts++;
    }

    for (Immortal im : population) {
      if (im != this && im.isAlive()) {
        return im;
      }
    }

    return null;
  }

  private void fightNaive(Immortal other) {
    synchronized (this) {
      synchronized (other) {
        if (this.health <= 0 || other.health <= 0) return;
        if (other.health < this.damage) return;

        other.health -= this.damage;
        this.health += this.damage / 2;
        scoreBoard.recordFight();
      }
    }
  }

  private void fightOrdered(Immortal other) {
    Immortal first = this.name.compareTo(other.name) < 0 ? this : other;
    Immortal second = this.name.compareTo(other.name) < 0 ? other : this;
    synchronized (first) {
      synchronized (second) {
        if (this.health <= 0 || other.health <= 0) return;
        if (other.health < this.damage) return; // No atacar si quedaría negativo

        other.health -= this.damage;
        this.health += this.damage / 2;
        scoreBoard.recordFight();
      }
    }
  }
}
