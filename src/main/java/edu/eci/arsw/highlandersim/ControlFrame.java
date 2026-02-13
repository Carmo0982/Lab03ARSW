package edu.eci.arsw.highlandersim;

import edu.eci.arsw.immortals.Immortal;
import edu.eci.arsw.immortals.ImmortalManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.List;

public final class ControlFrame extends JFrame {

  private ImmortalManager manager;
  private final JTextArea output = new JTextArea(14, 40);
  private final JButton startBtn = new JButton("Start");
  private final JButton pauseAndCheckBtn = new JButton("Pause & Check");
  private final JButton resumeBtn = new JButton("Resume");
  private final JButton stopBtn = new JButton("Stop");
  private final JButton removeDeadBtn = new JButton("Remove Dead");

  private final JSpinner countSpinner = new JSpinner(new SpinnerNumberModel(8, 2, 20000, 1));
  private final JSpinner healthSpinner = new JSpinner(new SpinnerNumberModel(100, 10, 10000, 10));
  private final JSpinner damageSpinner = new JSpinner(new SpinnerNumberModel(10, 1, 1000, 1));
  private final JComboBox<String> fightMode = new JComboBox<>(new String[]{"ordered", "naive"});

  private int initialCount;
  private int initialHealthPerImmortal;
  private int damagePerFight;

  public ControlFrame(int count, String fight) {
    setTitle("Highlander Simulator — ARSW");
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    setLayout(new BorderLayout(8,8));

    JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
    top.add(new JLabel("Count:"));
    countSpinner.setValue(count);
    top.add(countSpinner);
    top.add(new JLabel("Health:"));
    top.add(healthSpinner);
    top.add(new JLabel("Damage:"));
    top.add(damageSpinner);
    top.add(new JLabel("Fight:"));
    fightMode.setSelectedItem(fight);
    top.add(fightMode);
    add(top, BorderLayout.NORTH);

    output.setEditable(false);
    output.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
    add(new JScrollPane(output), BorderLayout.CENTER);

    JPanel bottom = new JPanel(new FlowLayout(FlowLayout.CENTER));
    bottom.add(startBtn);
    bottom.add(pauseAndCheckBtn);
    bottom.add(resumeBtn);
    bottom.add(removeDeadBtn);
    bottom.add(stopBtn);
    add(bottom, BorderLayout.SOUTH);

    startBtn.addActionListener(this::onStart);
    pauseAndCheckBtn.addActionListener(this::onPauseAndCheck);
    resumeBtn.addActionListener(this::onResume);
    removeDeadBtn.addActionListener(this::onRemoveDead);
    stopBtn.addActionListener(this::onStop);

    pack();
    setLocationByPlatform(true);
    setVisible(true);
  }

  private void onStart(ActionEvent e) {
    safeStop();
    int n = (Integer) countSpinner.getValue();
    int health = (Integer) healthSpinner.getValue();
    int damage = (Integer) damageSpinner.getValue();
    String fight = (String) fightMode.getSelectedItem();

    initialCount = n;
    initialHealthPerImmortal = health;
    damagePerFight = damage;

    manager = new ImmortalManager(n, fight, health, damage);
    manager.start();
    output.setText("Simulation started with %d immortals (health=%d, damage=%d, fight=%s)%n"
      .formatted(n, health, damage, fight));
  }

  private void onPauseAndCheck(ActionEvent e) {
    if (manager == null) return;
    manager.pause();

    try {
      Thread.sleep(50);
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
    }

    List<Immortal> pop = manager.populationSnapshot();
    long sum = 0;
    int aliveCount = 0;
    int negativeHealthCount = 0;

    StringBuilder sb = new StringBuilder();

    boolean showIndividual = pop.size() <= 50;

    for (Immortal im : pop) {
      int h = im.getHealth();
      sum += h;
      if (h > 0) aliveCount++;
      if (h < 0) negativeHealthCount++;

      if (showIndividual) {
        sb.append(String.format("%-14s : %5d%n", im.name(), h));
      }
    }

    if (!showIndividual) {
      sb.append(String.format("(Showing summary for %d immortals)%n", pop.size()));
    }

    sb.append("================================\n");
    sb.append(String.format("Population size  : %d%n", pop.size()));
    sb.append(String.format("Immortals alive  : %d / %d%n", aliveCount, pop.size()));
    sb.append(String.format("Negative health  : %d%n", negativeHealthCount));
    sb.append("--------------------------------\n");

    long totalFights = manager.scoreBoard().totalFights();
    sb.append(String.format("Total Fights     : %d%n", totalFights));
    sb.append(String.format("Current Health   : %d%n", sum));

    sb.append("--------------------------------\n");
    long initialTotal = (long) initialCount * initialHealthPerImmortal;
    long expectedTotal = initialTotal - (totalFights * (damagePerFight / 2));
    long difference = sum - expectedTotal;

    sb.append(String.format("Initial Total    : %d%n", initialTotal));
    sb.append(String.format("Expected Health  : %d%n", expectedTotal));
    sb.append(String.format("Difference       : %d%n", difference));

    long tolerance = Math.max(damagePerFight * 2, initialCount / 10);
    boolean invariantOK = Math.abs(difference) <= tolerance;

    sb.append("--------------------------------\n");
    sb.append(String.format("Invariant Status : %s%n",
      invariantOK ? "✓ OK" : "✗ VIOLATED"));

    if (!invariantOK) {
      sb.append(String.format("⚠ WARNING: Difference (%d) exceeds tolerance (%d)%n",
        Math.abs(difference), tolerance));
      sb.append("→ Check for race conditions in fight methods!%n");
    }

    if (negativeHealthCount > 0) {
      sb.append(String.format("⚠ WARNING: %d immortals with negative health!%n", negativeHealthCount));
      sb.append("→ Race condition detected in synchronized blocks!%n");
    }

    output.setText(sb.toString());
  }

  private void onResume(ActionEvent e) {
    if (manager == null) return;
    manager.resume();
  }

  private void onRemoveDead(ActionEvent e) {
    if (manager == null) return;
    manager.pause();

    try {
      Thread.sleep(50);
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
    }

    int beforeSize = manager.populationSize();
    int removed = manager.removeDeadImmortals();
    int afterSize = manager.populationSize();
    int alive = manager.aliveCount();

    StringBuilder sb = new StringBuilder();
    sb.append("=== Dead Immortals Removed ===\n");
    sb.append(String.format("Population before: %d%n", beforeSize));
    sb.append(String.format("Removed (dead):    %d%n", removed));
    sb.append(String.format("Population after:  %d%n", afterSize));
    sb.append(String.format("Still alive:       %d%n", alive));
    sb.append("==============================\n");
    sb.append("Note: Simulation is paused.\n");
    sb.append("Click 'Resume' to continue.\n");

    output.setText(sb.toString());
  }

  private void onStop(ActionEvent e) { safeStop(); }

  private void safeStop() {
    if (manager != null) {
      manager.stop();
      manager = null;
    }
  }

  public static void main(String[] args) {
    int count = Integer.getInteger("count", 8);
    String fight = System.getProperty("fight", "ordered");
    SwingUtilities.invokeLater(() -> new ControlFrame(count, fight));
  }
}
