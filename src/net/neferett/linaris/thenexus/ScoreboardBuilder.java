package net.neferett.linaris.thenexus;

import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;

public class ScoreboardBuilder {
    private static Random RANDOM = new Random();

    public static ScoreboardBuilder builder(final String name, final String displayName, final DisplaySlot display) {
        if (name.length() > 16) { throw new IllegalArgumentException("Name > 16"); }
        final Scoreboard scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        final Objective objective = scoreboard.registerNewObjective(name, "dummy");
        objective.setDisplayName(displayName);
        objective.setDisplaySlot(display);
        return new ScoreboardBuilder(scoreboard, objective);
    }

    public static ScoreboardBuilder from(final String name, final Scoreboard scoreboard) {
        final Objective objective = scoreboard.getObjective(name);
        return new ScoreboardBuilder(scoreboard, objective);
    }

    private final Scoreboard scoreboard;
    private final Objective objective;

    private ScoreboardBuilder(Scoreboard scoreboard, Objective objective) {
        this.scoreboard = scoreboard;
        this.objective = objective;
    }

    public ScoreboardBuilder addLines(int score, String... lines) {
        for (String line : lines) {
            this.setLine(--score, line);
        }
        return this;
    }

    public Scoreboard getScoreboard() {
        return scoreboard;
    }

    public ScoreboardBuilder setLine(int score, String line) {
        if (line.length() > 32 || line.length() == 0) { throw new IllegalArgumentException("line.length > 32 OR line.length = 0"); }
        ChatColor lineColor = ChatColor.values()[score < 0 ? Math.abs(score) : score];
        Team team = scoreboard.getTeam(lineColor.name());
        if (team == null) {
            team = scoreboard.registerNewTeam(lineColor.name());
        }
        if (team.getEntries().size() == 0) {
            team.addEntry(lineColor.toString());
        }
        String[] parts = Iterables.toArray(Splitter.fixedLength(16).split(line), String.class);
        if (parts.length == 2) {
            int lastIndex = parts[0].lastIndexOf("§");
            boolean addColor = parts[0].endsWith("§");
            team.setPrefix(addColor ? parts[0].substring(0, parts[0].length() - 1) : parts[0]);
            team.setSuffix((lastIndex == -1 ? ChatColor.RESET : addColor ? "§" : parts[0].substring(lastIndex, lastIndex + 2)) + parts[1]);
        } else if (parts.length == 1) {
            team.setPrefix(parts[0]);
            ChatColor suffix = null;
            while (suffix == null || suffix == lineColor) {
                suffix = ChatColor.values()[ScoreboardBuilder.RANDOM.nextInt(ChatColor.values().length)];
            }
            team.setSuffix(suffix.toString());
        }
        objective.getScore(lineColor.toString()).setScore(score);
        return this;
    }

    public ScoreboardBuilder removeLine(int score) {
        ChatColor lineColor = ChatColor.values()[score < 0 ? Math.abs(score) : score];
        Team team = scoreboard.getTeam(lineColor.name());
        if (team != null) {
            team.setPrefix("");
            team.setSuffix("");
            for (String entry : team.getEntries()) {
                team.removeEntry(entry);
                scoreboard.resetScores(entry);
            }
        }
        return this;
    }

    public ScoreboardBuilder removeLines(int... scores) {
        for (int score : scores) {
            this.removeLine(score);
        }
        return this;
    }
}
