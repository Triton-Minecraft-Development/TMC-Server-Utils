package tmc.util;

import it.unimi.dsi.fastutil.ints.IntArrayFIFOQueue;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.minecraft.world.phys.Vec3;

import java.util.Arrays;
import java.util.List;

public class DBSCAN {

    public static final int NOISE = -1;
    private static final int UNVISITED = -2;

    public static int[] cluster(List<Vec3> points, double eps, int minPts)
    {
        final int n = points.size();
        final double eps2 = eps * eps;

        // Neighbor table. Each list includes the point itself, so that minPts counts it.
        IntArrayList[] neighbours = new IntArrayList[n];
        for (int i = 0; i < n; i++)
        {
            neighbours[i] = new IntArrayList();
            neighbours[i].add(i);
        }
        for (int i = 0; i < n; i++)
        {
            Vec3 a = points.get(i);
            for (int j = i + 1; j < n; j++)
            {
                if (a.distanceToSqr(points.get(j)) > eps2)
                {
                    continue;
                }
                neighbours[i].add(j);
                neighbours[j].add(i);
            }
        }

        int[] labels = new int[n];
        Arrays.fill(labels, UNVISITED);
        int clusterId = 0;
        IntArrayFIFOQueue queue = new IntArrayFIFOQueue();

        for (int i = 0; i < n; i++)
        {
            if (labels[i] != UNVISITED)
            {
                continue;
            }
            if (neighbours[i].size() < minPts)
            {
                labels[i] = NOISE;
                continue;
            }

            // i is a core point, so start a new cluster and expand it breadth-first
            labels[i] = clusterId;
            neighbours[i].forEach(queue::enqueue);

            while (!queue.isEmpty())
            {
                int j = queue.dequeueInt();

                if (labels[j] == NOISE)
                {
                    labels[j] = clusterId; // border point: claim, but don't expand from here
                    continue;
                }
                if (labels[j] != UNVISITED)
                {
                    continue;
                }
                labels[j] = clusterId;
                if (neighbours[j].size() >= minPts)
                {
                    neighbours[j].forEach(queue::enqueue); // j is also core. keep growing
                }
            }
            clusterId++;
        }
        return labels;
    }

    private DBSCAN() {}
}
