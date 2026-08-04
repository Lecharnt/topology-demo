package com.myproject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Path;
import org.graphstream.ui.spriteManager.Sprite;
import org.graphstream.ui.spriteManager.SpriteManager;
import org.graphstream.graph.Node;

public class PacketAnimator {

    private final SpriteManager spriteManager;
    private final ScheduledExecutorService executor;

    private final List<AnimatedPacket> packets = new ArrayList<>();

    private int nextId = 0;
    private double speed = 0.01;
    private int tickTime = 20;

    public PacketAnimator(Graph graph) {
        spriteManager = new SpriteManager(graph);

        executor = Executors.newSingleThreadScheduledExecutor();

        executor.scheduleAtFixedRate(
                this::update,
                0,
                tickTime,
                TimeUnit.MILLISECONDS);
    }

    public void stop() {
        executor.shutdownNow();
    }

    public void setSpeed(double speed) {
        this.speed = speed;
    }

    public void spawn(Path path) {

        if (path == null || path.getEdgeCount() == 0)
            return;

        Sprite sprite = spriteManager.addSprite("packet" + nextId++);


        Edge first = path.getEdgePath().get(0);
        Node startNode = path.getNodePath().get(0);

        Node targetNode;
        sprite.setAttribute(
            "ui.style",
            "shape:circle;" +
            "size:8px;" +
            "fill-color:red;" +
            "text-size:12px;" +
            "text-color:black;"
        );
        if (first.getSourceNode().equals(startNode)) {
            targetNode = first.getTargetNode();
        } else {
            targetNode = first.getSourceNode();
        }

        sprite.setAttribute(
            "ui.label",
            startNode.getId() + " -> " + targetNode.getId()
        );
        
        sprite.attachToEdge(first.getId());

        boolean reverse;

        if(first.getSourceNode().equals(startNode)) {
            reverse = false;
        }
        else if(first.getTargetNode().equals(startNode)) {
            reverse = true;
        }
        else {
            System.out.println("First edge does not touch start node");
            return;
        }
        sprite.setPosition(reverse ? 1.0 : 0.0);

        packets.add(new AnimatedPacket(sprite, path, reverse));
    }

    private void update() {

        Iterator<AnimatedPacket> it = packets.iterator();

        while (it.hasNext()) {

            AnimatedPacket packet = it.next();

            packet.position += speed;

            if (packet.position >= 1.0) {

                packet.edgeIndex++;

                if (packet.edgeIndex >= packet.path.getEdgeCount()) {

                    spriteManager.removeSprite(packet.sprite.getId());
                    it.remove();
                    continue;
                }

                packet.position = 0;

                Edge edge = packet.path.getEdgePath()
                        .get(packet.edgeIndex);

                Node previous =
                        packet.path.getNodePath()
                        .get(packet.edgeIndex);

                Node next =
                        packet.path.getNodePath()
                        .get(packet.edgeIndex + 1);


                packet.sprite.attachToEdge(edge.getId());


                if (edge.getSourceNode().equals(previous)
                        && edge.getTargetNode().equals(next)) {

                    packet.reverse = false;

                } else if (edge.getTargetNode().equals(previous)
                        && edge.getSourceNode().equals(next)) {

                    packet.reverse = true;

                } else {

                    System.out.println(
                        "BAD EDGE\n" +
                        previous.getId() +
                        " -> " +
                        next.getId() +
                        " but edge is " +
                        edge.getSourceNode().getId() +
                        " -> " +
                        edge.getTargetNode().getId()
                    );
                }
            }

            if (packet.reverse) {
                packet.sprite.setPosition(1.0 - packet.position);
            } else {
                packet.sprite.setPosition(packet.position);
            }
        }
    }
    private static class AnimatedPacket {

        Sprite sprite;
        Path path;

        int edgeIndex = 0;
        double position = 0;

        boolean reverse;

        AnimatedPacket(Sprite sprite, Path path, boolean reverse) {
            this.sprite = sprite;
            this.path = path;
            this.reverse = reverse;
        }
    }
}