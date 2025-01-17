package space.invaders.gui;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import space.invaders.dto.AlienDto;
import space.invaders.dto.BulletDto;
import space.invaders.dto.GameStateDto;
import space.invaders.dto.PlayerDto;
import space.invaders.gamestate.Game;

import java.io.Serializable;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

public class GUI extends AbstractActor {
    private Stage stage;
    private ImageView player;
    private Pane root;
    private boolean playing = false;
    private Label livesLabel;
    private Label gameoverLabel;
    private Map<Integer, Circle> idToBullets = new HashMap<>();
    private Map<Integer, ImageView> idToAliens = new HashMap<>();
    private Map<String, Image> urlToImage = new HashMap<>();

    private static final int width = GameStateDto.screenSize.width;
    private static final int height = GameStateDto.screenSize.height;
    private ActorRef game;

    public static class GameInitialized implements Serializable {
        final ActorRef game;

        public GameInitialized(ActorRef game) {
            this.game = game;
        }
    }

    static public Props props() {
        return Props.create(GUI.class, GUI::new);
    }

    private GUI(){
        createLivesLabel();
        createGameOverLabel();
        start();
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(GameInitialized.class, gameInitialized -> this.game = gameInitialized.game)
                .match(Game.Start.class, start -> start())
                .match(GameStateDto.class, this::updateState)
                .build();
    }

    private void createLivesLabel(){
        livesLabel = new Label();
        livesLabel.getStyleClass().add("lives-label");
        livesLabel.setLayoutX(10);
        livesLabel.setLayoutY(height- 20);
        livesLabel.toFront();
    }

    private void createGameOverLabel() {
        gameoverLabel = new Label();
        gameoverLabel.getStyleClass().add("gameover-label");
    }

    private Circle createBullet(BulletDto bulletDto) {
        Circle bullet = new Circle();
        bullet.setCenterX(bulletDto.posX);
        bullet.setCenterY(bulletDto.posY);
        bullet.setRadius(2.0);
        String style = bulletDto.sender.equals(BulletDto.Sender.Player) ? "player-bullet" : "alien-bullet";
        bullet.getStyleClass().add(style);
        return bullet;
    }

    private Image createImage(String url){
        return new Image(Objects.requireNonNull(this.getClass().getClassLoader().getResourceAsStream(url)));
    }

    private ImageView createAlien(AlienDto alien){
        ImageView imageview = new ImageView(urlToImage.computeIfAbsent(alien.image.imageUrl, this::createImage));
        imageview.setLayoutX(alien.posX);
        imageview.setLayoutY(alien.posY);
        imageview.setFitWidth(alien.image.width);
        imageview.setPreserveRatio(true);
        return imageview;
    }

    private ImageView createPlayer(PlayerDto player){
        ImageView imageView = new ImageView(urlToImage.computeIfAbsent(player.image.imageUrl, this::createImage));
        imageView.setFitWidth(player.image.width);
        imageView.setLayoutX(player.posX);
        imageView.setLayoutY(player.posY);
        imageView.setPreserveRatio(true);
        return imageView;
    }

    private Scene getStartScene() {
        root = new StackPane();
        Button btn = new Button();
        btn.setText("Start game");
        btn.setOnAction(event -> game.tell(new Game.Start(), getSelf()));
        root.getChildren().add(btn);
        return new Scene(root, width, height);
    }

    private void changeScene(Scene scene) {
        scene.getStylesheets().add("style.css");
        stage.setScene(scene);
    }

    private Scene getGameScene() {
        root = new Pane();
        root.getChildren().add(livesLabel);
        Scene scene = new Scene(root, width, height);
        scene.addEventFilter(KeyEvent.KEY_PRESSED, keyEvent -> {
            switch (keyEvent.getCode()){
                case RIGHT: game.tell(new Game.MoveRight(), getSelf());
                    break;
                case LEFT: game.tell(new Game.MoveLeft(), getSelf());
                    break;
                case SPACE:game.tell(new Game.Fire(), getSelf());
                default:break;
            }
        });
        return scene;
    }

    private void start() {

        stage = new Stage();
        stage.setTitle("Space invaders");
        changeScene(getStartScene());
        stage.setResizable(false);
        stage.setOnCloseRequest(event -> System.exit(0));
        stage.show();
    }

    private void addGameOverLabel(String text){
        gameoverLabel.setText(text);
        if (!root.getChildren().contains(gameoverLabel)) {
            root.getChildren().add(gameoverLabel);
            root.applyCss();
            root.layout();
        }
        gameoverLabel.toFront();
        gameoverLabel.setLayoutX(width / 2.0 - gameoverLabel.getWidth() / 2);
        gameoverLabel.setLayoutY(height / 2.0 - gameoverLabel.getHeight() / 2);
    }

    private void updateState(GameStateDto gameStateDto) {
        switch (gameStateDto.state) {
            case Playing:
                if(!playing){
                    changeScene(getGameScene());
                    playing = true;
                }
                if(gameStateDto.player != null) {
                    updatePlayer(gameStateDto.player);
                }
                updateBullets(gameStateDto.bullets);
                updateAliens(gameStateDto.aliens);
                break;
            case GameLost:
                player.setLayoutX(gameStateDto.player.posX);
                livesLabel.setText("Lives: " + gameStateDto.player.lives);
                addGameOverLabel("Game over - you lost!");
                break;
            case GameWon:
                addGameOverLabel("Game over - you won!");
                break;
        }
    }

    private void updatePlayer(PlayerDto playerDto){
        if (player == null) {
            player = createPlayer(playerDto);
            root.getChildren().add(player);
        } else {
            player.setLayoutX(playerDto.posX);
        }
        livesLabel.setText("Lives: " + playerDto.lives);
    }

    private void updateBullets(List<BulletDto> bullets) {
        Function<BulletDto, BiFunction<Integer, Circle, Circle>> ifPresent =
                bullet -> (id, circle) -> {
                    circle.setCenterX(bullet.posX);
                    circle.setCenterY(bullet.posY);
                    return circle;
                };
        Function<BulletDto, Function<Integer, Circle>> ifAbsent =
                bullet -> id -> {
                    Circle newBullet = createBullet(bullet);
                    root.getChildren().add(newBullet);
                    return newBullet;
                };
        updateMap(
                idToBullets,
                bullets,
                bullet -> bullet.id,
                ifPresent,
                ifAbsent);
    }

    private void updateAliens(List<AlienDto> aliens) {
        Function<AlienDto, BiFunction<Integer, ImageView, ImageView>> ifPresent =
                alien -> (id, img) -> {
                    img.setImage(urlToImage.computeIfAbsent(alien.image.imageUrl, this::createImage));
                    img.setLayoutX(alien.posX);
                    img.setLayoutY(alien.posY);
                    return img;
                };
        Function<AlienDto, Function<Integer, ImageView>> ifAbsent =
                alien -> id -> {
                    ImageView newAlien = createAlien(alien);
                    root.getChildren().add(newAlien);
                    return newAlien;
                };

        updateMap(
                idToAliens,
                aliens,
                alien -> alien.id,
                ifPresent,
                ifAbsent);
    }

    private <S extends Node, T> void updateMap(
            Map<Integer, S> map,
            List<T> dtos,
            Function<T, Integer> getDtoId,
            Function<T, BiFunction<Integer, S, S>> ifPresent,
            Function<T, Function<Integer, S>> ifAbsent){

        // Remove items that does not exist anymore
        Set<Integer> keys = map.keySet();
        List<Integer> dtoIds = dtos.stream().map(getDtoId).collect(Collectors.toList());
        keys.forEach( id -> {
            if (!dtoIds.contains(id)) {
                root.getChildren().remove(map.get(id));
            }
        });

        // Add or update from dto
        for(final T dto : dtos){
            S result = map.computeIfPresent(getDtoId.apply(dto), ifPresent.apply(dto));
            if (result == null) {
                map.computeIfAbsent(getDtoId.apply(dto), ifAbsent.apply(dto));
            }
        }

    }
}
