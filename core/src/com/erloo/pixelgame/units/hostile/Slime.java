package com.erloo.pixelgame.units.hostile;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.erloo.pixelgame.pathfinding.Node;
import com.erloo.pixelgame.Player;
import com.erloo.pixelgame.damage.Damageable;
import com.badlogic.gdx.math.Rectangle;
import com.erloo.pixelgame.pathfinding.Pathfinder;
import com.erloo.pixelgame.pathfinding.Grid;

import java.util.List;

public class Slime extends Enemy implements Damageable {
    private Vector2 position;
    private Animation<TextureRegion> currentAnimation;
    private float stateTime;
    private TextureRegion currentFrame;
    private TextureAtlas atlas;
    private float viewRadius;
    private boolean isChasing;
    private Vector2 target;
    private Vector2 spawnPosition;
    private Array<TiledMapTileLayer> collisionLayers;
    private boolean isCollidingWithPlayer;
    private Pathfinder pathfinder;
    private Grid grid;
    private Player player;
    private TextureRegion frontFrame;
    private TextureRegion backFrame;
    private TextureRegion leftFrame;
    private TextureRegion rightFrame;

    public Slime(TextureAtlas atlas, int damage, Vector2 position, Array<TiledMapTileLayer> collisionLayers, Grid grid, Player player) {
        super(damage, player);
        this.position = position;
        this.spawnPosition = position.cpy();
        this.atlas = atlas;
        this.collisionLayers = collisionLayers;
        this.grid = grid;
        this.player = player;
        viewRadius = 60f;
        isChasing = false;
        createAnimations();
        currentAnimation = frontAnimation;
        health = 30; // устанавливаем максимальное здоровье
        pathfinder = new Pathfinder();
    }

    public void update(float delta) {
        boolean wasMoving = isMoving(); // сохраняем предыдущее значение isMoving
        isMoving = false; // сбрасываем isMoving в false

        setInvulnerable(delta);
        if (!isCollidingWithPlayer) {
            if (isChasing) {
                List<Node> path = findPathToPlayer(player);
                if (path != null && !path.isEmpty()) {
                    Node nextNode = path.get(0);
                    Vector2 nextPosition = new Vector2(nextNode.x * 16, nextNode.y * 16);
                    Vector2 direction = nextPosition.cpy().sub(position).nor();
                    float speed = 40f;

                    float newX = position.x;
                    float newY = position.y;

                    // Move along X-axis
                    newX += direction.x * speed * delta;
                    position.x = newX;

                    newY += direction.y * speed * delta;
                    position.y = newY;

//                    if (!isCellOccupied(newX, position.y)) {
//                        position.x = newX;
//                    } else {
//                        System.out.println("Slime hit an obstacle!");
//                    }


//                    if (!isCellOccupied(position.x, newY)) {
//                        position.y = newY;
//                    } else {
//                        System.out.println("Slime hit an obstacle!");
//                    }

                    // Update the current animation based on the direction of movement
                    if (Math.abs(direction.x) > Math.abs(direction.y)) {
                        if (direction.x > 0) {
                            currentAnimation = rightAnimation;
                        } else if (direction.x < 0) {
                            currentAnimation = leftAnimation;
                        }
                    } else {
                        if (direction.y > 0) {
                            currentAnimation = backAnimation;
                        } else if (direction.y < 0) {
                            currentAnimation = frontAnimation;
                        }
                    }

                    if (position.epsilonEquals(nextPosition, 1f)) {
                        path.remove(0);
                    }

                    isMoving = true; // устанавливаем isMoving в true, если враг двигается
                }
            } else {
                if (!position.epsilonEquals(spawnPosition, 1f)) {
                    Vector2 direction = spawnPosition.cpy().sub(position).nor();
                    float speed = 40f;

                    float newX = position.x + direction.x * speed * delta;
                    float newY = position.y + direction.y * speed * delta;

                    position.x = newX;
                    position.y = newY;

//                    if (!isCellOccupied(newX, newY)) {
//                        position.x = newX;
//                        position.y = newY;
//                    }

                    // Update the current animation based on the direction of movement
                    if (Math.abs(direction.x) > Math.abs(direction.y)) {
                        if (direction.x > 0) {
                            currentAnimation = rightAnimation;
                        } else if (direction.x < 0) {
                            currentAnimation = leftAnimation;
                        }
                    } else {
                        if (direction.y > 0) {
                            currentAnimation = backAnimation;
                        } else if (direction.y < 0) {
                            currentAnimation = frontAnimation;
                        }
                    }

                    isMoving = true; // устанавливаем isMoving в true, если враг двигается
                } else {
                    currentAnimation = frontAnimation;
                }
            }
        }

        if (wasMoving != isMoving()) { // обновляем stateTime только при изменении значения isMoving
            stateTime = 0;
        }
    }

    public void render(SpriteBatch slimeBatch) {
        stateTime += Gdx.graphics.getDeltaTime();

        if (isCollidingWithPlayer) {
            if (currentAnimation == frontAnimation) {
                currentFrame = frontFrame;
            } else if (currentAnimation == backAnimation) {
                currentFrame = backFrame;
            } else if (currentAnimation == leftAnimation) {
                currentFrame = leftFrame;
            } else if (currentAnimation == rightAnimation) {
                currentFrame = rightFrame;
            }
        } else {
            if (isMoving()) { // обновляем анимацию только при движении
                currentFrame = currentAnimation.getKeyFrame(stateTime, true);
            } else {
                // устанавливаем первый кадр frontAnimation при остановке
                currentFrame = frontAnimation.getKeyFrame(0);
            }
        }

        int slimeWidth = currentFrame.getRegionWidth();
        int slimeHeight = currentFrame.getRegionHeight();

        blinking(slimeBatch);
        slimeBatch.draw(currentFrame, position.x - slimeWidth / 2, position.y - slimeHeight / 2);
    }
    public void setMoving(boolean moving) { // переопределяем сеттер для isMoving в классе Slime
        super.setMoving(moving);
    }
    public float getPositionX() {
        return position.x;
    }

    public float getPositionY() {
        return position.y;
    }
    public int getWidth() {
        return currentFrame.getRegionWidth();
    }
    public int getHeight() {
        return currentFrame.getRegionHeight();
    }
    public void checkCollisionWithPlayer(Player player) {
        Rectangle slimeRect = getBoundingRectangle();
        Rectangle playerRect = player.getBoundingRectangle();
        if (Intersector.overlaps(slimeRect, playerRect)) {
            isCollidingWithPlayer = true;
            isChasing = false;
        } else {
            isCollidingWithPlayer = false;
        }
    }
    public Vector2 getPosition() {
        return position;
    }
    public boolean isCellOccupied(float x, float y) {
        for (TiledMapTileLayer layer : collisionLayers) {
            int cellX = (int) (x / 16);
            int cellY = (int) (y / 16);
            if (cellX >= 0 && cellX < layer.getWidth() && cellY >= 0 && cellY < layer.getHeight()) {
                TiledMapTileLayer.Cell cell = layer.getCell(cellX, cellY);
                if (cell != null && cell.getTile() != null) {
                    return true;
                }
            }
        }
        return false;
    }
    @Override
    public void reward(){
        int COIN_REWARD = 5;
        player.getCoins().addCoins(COIN_REWARD);
    }
    public void checkTargetInView(Vector2 target) {
        if (position.dst(target) <= viewRadius) {
            isChasing = true;
            this.target = target;
        } else {
            isChasing = false;
        }
    }
    public int getHealth() {
        return health;
    }
    public Rectangle getBoundingRectangle() {
        return new Rectangle(position.x, position.y, currentFrame.getRegionWidth(), currentFrame.getRegionHeight());
    }

    private Animation<TextureRegion> leftAnimation;
    private Animation<TextureRegion> rightAnimation;
    private Animation<TextureRegion> backAnimation;
    private Animation<TextureRegion> frontAnimation;

    private void createAnimations() {
        Array<TextureAtlas.AtlasRegion> leftFrames = new Array<>();
        leftFrames.add(atlas.findRegion("left1"));
        leftFrames.add(atlas.findRegion("left2"));
        leftAnimation = new Animation<>(0.3f, leftFrames, Animation.PlayMode.LOOP);

        Array<TextureAtlas.AtlasRegion> rightFrames = new Array<>();
        rightFrames.add(atlas.findRegion("right1"));
        rightFrames.add(atlas.findRegion("right2"));
        rightAnimation = new Animation<>(0.3f, rightFrames, Animation.PlayMode.LOOP);

        Array<TextureAtlas.AtlasRegion> backFrames = new Array<>();
        backFrames.add(atlas.findRegion("back1"));
        backFrames.add(atlas.findRegion("back2"));
        backAnimation = new Animation<>(0.3f, backFrames, Animation.PlayMode.LOOP);

        Array<TextureAtlas.AtlasRegion> frontFrames = new Array<>();
        frontFrames.add(atlas.findRegion("front1"));
        frontFrames.add(atlas.findRegion("front2"));
        frontAnimation = new Animation<>(0.3f, frontFrames, Animation.PlayMode.LOOP);

        // Инициализируем переменные для хранения первых кадров каждой анимации
        frontFrame = frontFrames.get(0);
        backFrame = backFrames.get(0);
        leftFrame = leftFrames.get(0);
        rightFrame = rightFrames.get(0);

        currentFrame = frontAnimation.getKeyFrame(0);
    }


    public Vector2 getSpawnPosition() {
        return spawnPosition;
    }

    public void stopMoving() {
        isChasing = false;
    }
    @Override
    public void deathmessage(){
        String message = "Slime is dead";
        System.out.println(message);
    }
    public List<Node> findPathToPlayer(Player player) {
        int playerGridX = (int) (player.getPosition().x / 16);
        int playerGridY = (int) (player.getPosition().y / 16);
        Node startNode = grid.getNode((int) (position.x / 16), (int) (position.y / 16));
        Node endNode = grid.getNode(playerGridX, playerGridY);
        return pathfinder.findPath(startNode, endNode, grid);
    }
}
