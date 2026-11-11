package tmc.util;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class PlayerClusterScanner {

    public static final double SCAN_RADIUS_ENDER_DRAGON = 200.0;

    /**
     * DBSCAN neighborhood radius. Keep below the dive's impact radius so that any
     * clusters found can actually be hit in one impact
     */
    public static final double EPS_ENDER_DRAGON_DIVE = 6.0;

    /**
     * Minimum amount of players within EPS to count as a group
     */
    public static final int MIN_PTS_ENDER_DRAGON_DIVE = 2;

    /**
     * Determines if a lone player should be dived on if nobody is grouped up enough
     */
    public static final boolean FALLBACK_TO_SINGLE_PLAYER_ENDER_DRAGON_DIVE = true;

    public static Optional<DiveTarget> findDiveTarget(ServerLevel level, double scanRadius, double eps, double minPts, boolean fallbackToSingle)
    {
        final double r2 = scanRadius * scanRadius;

        List<ServerPlayer> candidates = level.getPlayers(player ->
        {
            return player.isAlive() && !player.isSpectator() && !player.isCreative() && ((player.getX() * player.getX() + player.getZ() * player.getZ()) <= r2);
        });

        if (candidates.isEmpty())
        {
            return Optional.empty();
        }

        List<Vec3> points = candidates.stream().map(Entity::position).toList();
        int[] labels = DBSCAN.cluster(points, EPS_ENDER_DRAGON_DIVE, MIN_PTS_ENDER_DRAGON_DIVE);

        // Group the player indices by cluster id. Skip noise
        Int2ObjectOpenHashMap<List<ServerPlayer>> clusters = new Int2ObjectOpenHashMap<>();
        for (int i = 0; i < labels.length; i++)
        {
            if (labels[i] == DBSCAN.NOISE)
            {
                continue;
            }
            clusters.computeIfAbsent(labels[i], k -> new ArrayList<>()).add(candidates.get(i));
        }

        // Densest = most players; ties got to the tighter group (smaller mean distance to centroid)
        Optional<DiveTarget> best = clusters.values().stream()
                .map(members ->
                {
                    Vec3 c = centroid(members);
                    return new DiveTarget(c, members, true, spread(members, c));
                })
                .max(Comparator.<DiveTarget>comparingInt(t -> t.members.size()).thenComparing(Comparator.comparingDouble(DiveTarget::spread).reversed()));

        if (best.isPresent() || !FALLBACK_TO_SINGLE_PLAYER_ENDER_DRAGON_DIVE)
        {
            return best;
        }

        // All noise: pick a random lone player
        ServerPlayer lone = candidates.get(level.getRandom().nextInt(candidates.size()));
        return Optional.of(new DiveTarget(lone.position(), List.of(lone), false, 0.0));
    }

    private static Vec3 centroid(List<ServerPlayer> players)
    {
        double x = 0, y = 0, z = 0;
        for (ServerPlayer player : players)
        {
            x += player.getX();
            y += player.getY();
            z += player.getZ();
        }
        int n = players.size();
        return new Vec3(x / n, y / n, z / n);
    }

    private static double spread(List<ServerPlayer> players, Vec3 c)
    {
        double sum = 0;
        for (ServerPlayer player : players)
        {
            sum += player.position().distanceTo(c);
        }
        return sum / players.size();
    }

    private PlayerClusterScanner() {}

    public record DiveTarget(Vec3 point, List<ServerPlayer> members, boolean isCluster, double spread) {}
}
