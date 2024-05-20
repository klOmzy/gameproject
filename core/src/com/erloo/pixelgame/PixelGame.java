package com.erloo.pixelgame;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;
import com.erloo.pixelgame.damage.Damager;
import com.erloo.pixelgame.damage.HealthBar;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.math.Vector2;
import com.erloo.pixelgame.dialogues.Dialogue;
import com.erloo.pixelgame.dialogues.DialogueBox;
import com.erloo.pixelgame.dialogues.DialogueManager;
import com.erloo.pixelgame.dialogues.DialogueOption;
import com.erloo.pixelgame.pathfinding.Grid;
import com.erloo.pixelgame.pathfinding.Node;
import com.erloo.pixelgame.units.Alice;
import com.erloo.pixelgame.units.Mage;
import com.erloo.pixelgame.units.Merchant;
import com.erloo.pixelgame.units.hostile.Ghost;
import com.erloo.pixelgame.units.hostile.Golem;
import com.erloo.pixelgame.units.hostile.Pillager;
import com.erloo.pixelgame.units.hostile.Slime;
import java.util.HashMap;

public class PixelGame extends ApplicationAdapter {
	private OrthographicCamera camera;
	private SpriteBatch batch;
	private SpriteBatch uiBatch; // Новый SpriteBatch для UI-элементов
	private TiledMap map;
	private TiledMap mapBackground;
	private OrthogonalTiledMapRenderer backgroundRenderer ;
	private OrthogonalTiledMapRenderer foregroundRenderer ;
	private Player player;
	private TextureAtlas playerAtlas;
	private float mapWidth;
	private float mapHeight;
	private float viewportWidth = 200;
	private float viewportHeight = 150;
	private Array<TiledMapTileLayer> collisionLayers;
	private float spawnX;
	private float spawnY;
	private HealthBar healthBar;
	private BitmapFont healthFont;
	private BitmapFont unitsHealthFont;
	private BitmapFont coinPotionFont;
	private BitmapFont dialogFont;
	private ShapeRenderer shapeRenderer;
	private BitmapFont deathFont;
	private Array<Damager> enemies;
	private SpriteBatch slimeBatch; // добавляем новый SpriteBatch
	private SpriteBatch ghostBatch; // Новый SpriteBatch для рендера призраков
	private SpriteBatch pillagerBatch; // Новый SpriteBatch для рендера призраков
	private SpriteBatch golemBatch; // Новый SpriteBatch для рендера призраков
	private SpriteBatch npcBatch;
	private HashMap<Slime, Float> slimeDeathTimes = new HashMap<>(); // добавляем переменную для хранения времени смерти слайма
	private HashMap<Ghost, Float> ghostDeathTimes = new HashMap<>(); // добавляем переменную для хранения времени смерти слайма
	private HashMap<Pillager, Float> pillagerDeathTimes = new HashMap<>(); // добавляем переменную для хранения времени смерти слайма
	private HashMap<Golem, Float> golemDeathTimes = new HashMap<>(); // добавляем переменную для хранения времени смерти слайма
	private Grid grid;
	private TextureAtlas aliceAtlas;
	private TextureAtlas mageAtlas;
	private TextureAtlas merchantAtlas;
	private Alice alice;
	private Mage mage;
	private Merchant merchant;
	private Vector2 aliceSpawnPosition;
	private Vector2 mageSpawnPosition;
	private Vector2 merchantSpawnPosition;
	private DialogueBox dialogueBox;
	private DialogueManager dialogueManager;
	private GameState state;
	private Menu menu;
	private int selectedMenuIndex;
	private Coin playerCoins;

	public enum GameState {
		MENU,
		PLAY,
		PAUSE
	}

	@Override
	public void create() {
		state = GameState.MENU;
		String[] menuOptions = new String[] {"Start Game", "Exit"};
		selectedMenuIndex = 0;
		menu = new Menu(this, menuOptions, selectedMenuIndex);
		playerCoins = new Coin();
		map = new TmxMapLoader().load("map.tmx");
		mapBackground = new TmxMapLoader().load("backgroundmap.tmx");

		collisionLayers = new Array<>();
		for (MapLayer layer : map.getLayers()) {
			if (layer instanceof TiledMapTileLayer && layer.getProperties().containsKey("collision")) {
				boolean isCollisionLayer = (boolean) layer.getProperties().get("collision");
				if (isCollisionLayer) {
					collisionLayers.add((TiledMapTileLayer) layer);
				}
			}
		}
		if (collisionLayers.size == 0) {
			throw new RuntimeException("Collision layers not found");
		}

		camera = new OrthographicCamera(viewportWidth, viewportHeight);
		camera.position.set(viewportWidth, viewportHeight, 0);
		camera.update();

		batch = new SpriteBatch();
		slimeBatch = new SpriteBatch(); // инициализируем новый SpriteBatch
		uiBatch = new SpriteBatch(); // Инициализируем новый SpriteBatch
		ghostBatch = new SpriteBatch(); // Инициализируем новый SpriteBatch
		pillagerBatch = new SpriteBatch(); // Инициализируем новый SpriteBatch
		golemBatch = new SpriteBatch(); // Инициализируем новый SpriteBatch
		npcBatch = new SpriteBatch();
		// Устанавливаем размеры камеры в соответствии с размерами окна
		camera.setToOrtho(false, viewportWidth, viewportHeight);

		MapObject spawnPoint = map.getLayers().get("Spawn").getObjects().get("SpawnPoint");
		if (spawnPoint != null) {
			spawnX = spawnPoint.getProperties().get("x", Float.class);
			spawnY = spawnPoint.getProperties().get("y", Float.class);
		} else {
			throw new RuntimeException("Spawn point not found");
		}

		backgroundRenderer = new OrthogonalTiledMapRenderer(mapBackground);
		foregroundRenderer = new OrthogonalTiledMapRenderer(map);

		playerAtlas = new TextureAtlas("player/player1.atlas"); // load the player atlas
		player = new Player(playerAtlas, collisionLayers, camera, spawnX, spawnY);

		mapWidth = map.getProperties().get("width", Integer.class) * map.getProperties().get("tilewidth", Integer.class);
		mapHeight = map.getProperties().get("height", Integer.class) * map.getProperties().get("tileheight", Integer.class);

		grid = new Grid((int) (mapWidth / 16), (int) (mapHeight / 16));
		for (int i = 0; i < grid.width; i++) {
			for (int j = 0; j < grid.height; j++) {
				Node node = grid.getNode(i, j);
				if (isCellOccupied(i * 16, j * 16)) { // предполагая, что метод isCellOccupied проверяет, занята ли клетка в слое коллизий
					node.walkable = false;
				} else {
					node.walkable = true;
				}
			}
		}

		grid.printGrid(); // выводим Grid в консоль
		enemies = new Array<Damager>();

		TextureAtlas slimes = new TextureAtlas("enemies/slime.atlas");
		MapLayer spawnLayer = map.getLayers().get("Spawn");
		for (MapObject object : spawnLayer.getObjects()) {
			if (object.getName().startsWith("Slime")) {
				float spawnX = object.getProperties().get("x", Float.class);
				float spawnY = object.getProperties().get("y", Float.class);
				Vector2 spawnPosition = new Vector2(spawnX, spawnY);
				Slime slime = new Slime(slimes, 5, spawnPosition, collisionLayers, grid, player);
				enemies.add(slime);
			}
		}

		TextureAtlas ghosts = new TextureAtlas("enemies/ghost.atlas");
		for (MapObject object : spawnLayer.getObjects()) {
			if (object.getName().startsWith("Ghost")) {
				float spawnX = object.getProperties().get("x", Float.class);
				float spawnY = object.getProperties().get("y", Float.class);
				Vector2 spawnPosition = new Vector2(spawnX, spawnY);
				Ghost ghost = new Ghost(ghosts, 35, spawnPosition, collisionLayers, grid, player);
				enemies.add(ghost);
			}
		}
		TextureAtlas pillagers = new TextureAtlas("enemies/pillager.atlas");
		for (MapObject object : spawnLayer.getObjects()) {
			if (object.getName().startsWith("Pillager")) {
				float spawnX = object.getProperties().get("x", Float.class);
				float spawnY = object.getProperties().get("y", Float.class);
				Vector2 spawnPosition = new Vector2(spawnX, spawnY);
				Pillager pillager = new Pillager(pillagers, 15, spawnPosition, collisionLayers, grid, player);
				enemies.add(pillager);
			}
		}
		TextureAtlas golems = new TextureAtlas("enemies/tilted_golem.atlas");
		for (MapObject object : spawnLayer.getObjects()) {
			if (object.getName().startsWith("Golem")) {
				float spawnX = object.getProperties().get("x", Float.class);
				float spawnY = object.getProperties().get("y", Float.class);
				Vector2 spawnPosition = new Vector2(spawnX, spawnY);
				Golem golem = new Golem (golems, 50, spawnPosition, collisionLayers, grid, player);
				enemies.add(golem);
			}
		}
		// Создаем генератор шрифтов из файла TTF
		FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("fonts/arial.ttf"));

		// Настраиваем параметры шрифта
		FreeTypeFontParameter parameter = new FreeTypeFontParameter();
		parameter.size = 16;
		parameter.color = Color.WHITE;

		// Генерируем BitmapFont из файла TTF
		healthFont = generator.generateFont(parameter);

		// Настраиваем параметры шрифта
		FreeTypeFontParameter unitsHealthParameter = new FreeTypeFontParameter();
		unitsHealthParameter.size = 16;
		unitsHealthParameter.color = Color.WHITE;

		// Генерируем BitmapFont из файла TTF
		unitsHealthFont = generator.generateFont(unitsHealthParameter);

		FreeTypeFontParameter coinparameter = new FreeTypeFontParameter();
		coinparameter.size = 16;
		coinparameter.color = Color.RED;

		coinPotionFont = generator.generateFont(coinparameter);

		// Настраиваем параметры шрифта
		FreeTypeFontParameter dialog = new FreeTypeFontParameter();
		dialog.size = 16;
		dialog.color = Color.WHITE;
		dialogFont = generator.generateFont(dialog);

		// Создаем новый FreeTypeFontParameter с большим размером шрифта для "YOU DIED!"
		FreeTypeFontParameter deathFontParameter = new FreeTypeFontParameter();
		deathFontParameter.size = 48; // Увеличиваем размер шрифта до 32
		deathFontParameter.color = Color.RED;

		// Генерируем новый BitmapFont для "YOU DIED!"
		deathFont = generator.generateFont(deathFontParameter);
		dialogueBox = new DialogueBox(dialogFont, alice, player);

		// Освобождаем ресурсы генератора шрифтов
		generator.dispose();

		shapeRenderer = new ShapeRenderer();
		healthBar = new HealthBar(10, 10, 200, 20, new Color(0.3f, 0.3f, 0.3f, 1), new Color(0.8f, 0.2f, 0.2f, 1), healthFont);

		MapObject aliceSpawnObject = map.getLayers().get("NPC_Spawn").getObjects().get("AliceSpawnPoint");
		if (aliceSpawnObject != null) {
			aliceSpawnPosition = new Vector2(aliceSpawnObject.getProperties().get("x", Float.class),
					aliceSpawnObject.getProperties().get("y", Float.class));
		} else {
			throw new RuntimeException("Alice spawn point not found");
		}

		MapObject mageSpawnObject = map.getLayers().get("NPC_Spawn").getObjects().get("MageSpawnPoint");
		if (mageSpawnObject != null) {
			mageSpawnPosition = new Vector2(mageSpawnObject.getProperties().get("x", Float.class),
					mageSpawnObject.getProperties().get("y", Float.class));
		} else {
			throw new RuntimeException("Mage spawn point not found");
		}

		MapObject merchantSpawnObject = map.getLayers().get("NPC_Spawn").getObjects().get("MerchantSpawnPoint");
		if (merchantSpawnObject != null) {
			merchantSpawnPosition = new Vector2(merchantSpawnObject.getProperties().get("x", Float.class),
					merchantSpawnObject.getProperties().get("y", Float.class));
		} else {
			throw new RuntimeException("Merchant spawn point not found");
		}
		//
		dialogueManager = new DialogueManager();
		DialogueOption byeoption1 = new DialogueOption("Good bye!", null, null);

		Array<DialogueOption> byeoption = new Array<>();
		byeoption.add(byeoption1);

		Dialogue rewardDialogue = new Dialogue("Yes, there is a great reward for the one who can \ndefeat the Dark Knight.", byeoption);
		Dialogue knightDialogue = new Dialogue("The Dark Knight is a powerful and evil being. He \nwas once a human, but was corrupted by a dark \nforce.", byeoption);

		DialogueOption option1 = new DialogueOption("I can handle it", null, null);
		DialogueOption option2 = new DialogueOption("Is there any reward for doing that?", rewardDialogue, null);
		DialogueOption option3 = new DialogueOption("Can you tell me more about that knight?", knightDialogue, null);

		Array<DialogueOption> options = new Array<>();
		options.add(option1);
		options.add(option2);
		options.add(option3);

		Dialogue firstDialogue = new Dialogue("The world has been corrupted. The Dark Knight \nkeeps the whole world at bay. " +
				"Humanity needs \nsomeone to defeat him.", options);

		dialogueManager.addDialogue("alice_first_dialogue", firstDialogue);
		dialogueManager.addDialogue("alice_reward_dialogue", rewardDialogue);
		dialogueManager.addDialogue("alice_knight_dialogue", knightDialogue);


		DialogueOption MageOption1Success = new DialogueOption("Thanks for a health potion!", null, null);
		Array<DialogueOption> MageOptionsSuccess = new Array<>();
		MageOptionsSuccess.add(MageOption1Success);
		Dialogue mageDiaSuccess = new Dialogue("Thank you for your purchase!", MageOptionsSuccess);
		dialogueManager.addDialogue("mage_success", mageDiaSuccess);

		DialogueOption MageOption1Fail = new DialogueOption("Oh, I'll be back later.", null, null);
		Array<DialogueOption> MageOptionsFail = new Array<>();
		MageOptionsFail.add(MageOption1Fail);
		Dialogue mageDiaFail = new Dialogue("You don't have enough coins to buy a health potion.", MageOptionsFail);
		dialogueManager.addDialogue("mage_fail", mageDiaFail);

		DialogueOption MageOption1 = new DialogueOption("Buy a health potion for 25 coins", null, () -> mage.purchaseHealthPotion());
		DialogueOption MageOption2 = new DialogueOption("No thanks, maybe later", null, null);

		Array<DialogueOption> MageOptions = new Array<>();
		MageOptions.add(MageOption1);
		MageOptions.add(MageOption2);

		Dialogue mageDia = new Dialogue("Welcome to my shop! I have health potions for sale. \nWould you like to buy one?", MageOptions);


		dialogueManager.addDialogue("magefirst", mageDia);

		DialogueOption MerchantOption1Success = new DialogueOption("Thanks for the upgrade!", null, null);
		Array<DialogueOption> MerchantOptionsSuccess = new Array<>();
		MerchantOptionsSuccess.add(MerchantOption1Success);
		Dialogue merchantDiaSuccess = new Dialogue("Thank you for your purchase!", MerchantOptionsSuccess);
		dialogueManager.addDialogue("merchant_success", merchantDiaSuccess);

		DialogueOption MerchantOption1Fail = new DialogueOption("Oh, I'll be back later.", null, null);
		Array<DialogueOption> MerchantOptionsFail = new Array<>();
		MerchantOptionsFail.add(MerchantOption1Fail);

		Dialogue merchantDiaFail1 = new Dialogue("You don't have enough coins to buy a HP upgrade.", MerchantOptionsFail);
		dialogueManager.addDialogue("merchant_fail1", merchantDiaFail1);

		Dialogue merchantDiaFail2 = new Dialogue("You don't have enough coins to buy a \ndamage upgrade.", MerchantOptionsFail);
		dialogueManager.addDialogue("merchant_fail2", merchantDiaFail2);

		DialogueOption MerchantOption1 = new DialogueOption("Buy a +25 HP for 25 coins", null, () -> merchant.purchaseUpgradeHP());
		DialogueOption MerchantOption2 = new DialogueOption("Buy a +5 damage for 25 coins", null, () -> merchant.purchaseUpgradeDamage());
		DialogueOption MerchantOption3 = new DialogueOption("No thanks, maybe later", null, null);

		Array<DialogueOption> MerchantOptions = new Array<>();
		MerchantOptions.add(MerchantOption1);
		MerchantOptions.add(MerchantOption2);
		MerchantOptions.add(MerchantOption3);

		Dialogue merchantDia = new Dialogue("Welcome to my shop! I can help you upgrade your \nstats!. Would you like some buffs?", MerchantOptions);


		dialogueManager.addDialogue("merchantfirst", merchantDia);


		aliceAtlas = new TextureAtlas("npc/alice.atlas");
		alice = new Alice(aliceAtlas, aliceSpawnPosition, player, dialogFont, dialogueBox, dialogueManager);

		mageAtlas = new TextureAtlas("npc/mage.atlas");
		mage = new Mage(mageAtlas, mageSpawnPosition, player, dialogFont, dialogueBox, dialogueManager);

		merchantAtlas = new TextureAtlas("npc/merchant.atlas");
		merchant = new Merchant(merchantAtlas, merchantSpawnPosition, player, dialogFont, dialogueBox, dialogueManager, playerCoins);

	}
	@Override
	public void render() {
		switch (state) {
			case MENU:
				Gdx.gl.glClearColor(0, 0, 0, 1);
				Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
				menu.update(); // добавляем этот вызов
				menu.render();
				break;
			case PLAY:
				Gdx.gl.glClearColor(1, 1, 1, 1);
				Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

				if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
					if (state == GameState.PLAY) {
						state = GameState.PAUSE;
					} else if (state == GameState.PAUSE) {
						state = GameState.PLAY;
					}
				}

				player.update(Gdx.graphics.getDeltaTime(), mapWidth, mapHeight);
				player.centerCamera();

				float cameraX = player.getPosition().x;
				float cameraY = player.getPosition().y;

				if (cameraX < viewportWidth / 2) {
					cameraX = viewportWidth / 2;
				} else if (cameraX > mapWidth - viewportWidth / 2) {
					cameraX = mapWidth - viewportWidth / 2;
				}

				if (cameraY < viewportHeight / 2) {
					cameraY = viewportHeight / 2;
				} else if (cameraY > mapHeight - viewportHeight / 2) {
					cameraY = mapHeight - viewportHeight / 2;
				}

				camera.position.set(cameraX, cameraY, 0);
				camera.update();

				batch.setProjectionMatrix(camera.combined);
				slimeBatch.setProjectionMatrix(camera.combined);
				ghostBatch.setProjectionMatrix(camera.combined);
				pillagerBatch.setProjectionMatrix(camera.combined);
				golemBatch.setProjectionMatrix(camera.combined);
				npcBatch.setProjectionMatrix(camera.combined);

				Array<Slime> temporarySlimes = new Array<Slime>(slimeDeathTimes.keySet().toArray(new Slime[0]));
				for (Slime slime : temporarySlimes) {
					slimeDeathTimes.put(slime, slimeDeathTimes.get(slime) + Gdx.graphics.getDeltaTime());
					if (slimeDeathTimes.get(slime) >= 10) { // если прошло 5 секунд после смерти слайма
						slimeDeathTimes.remove(slime); // удаляем слайма из списка мертвых слаймов
						spawnSlime(slime.getSpawnPosition()); // респавним слайма
					}
				}

				Array<Ghost> temporaryGhost = new Array<Ghost>(ghostDeathTimes.keySet().toArray(new Ghost[0]));
				for (Ghost ghost : temporaryGhost) {
					ghostDeathTimes.put(ghost, ghostDeathTimes.get(ghost) + Gdx.graphics.getDeltaTime());
					if (ghostDeathTimes.get(ghost) >= 10) { // если прошло 5 секунд после смерти слайма
						ghostDeathTimes.remove(ghost); // удаляем слайма из списка мертвых слаймов
						spawnGhost(ghost.getSpawnPosition()); // респавним слайма
					}
				}

				Array<Pillager> temporaryPillagers = new Array<Pillager>(pillagerDeathTimes.keySet().toArray(new Pillager[0]));
				for (Pillager pillager : temporaryPillagers) {
					pillagerDeathTimes.put(pillager, pillagerDeathTimes.get(pillager) + Gdx.graphics.getDeltaTime());
					if (pillagerDeathTimes.get(pillager) >= 10) { // если прошло 5 секунд после смерти слайма
						pillagerDeathTimes.remove(pillager); // удаляем слайма из списка мертвых слаймов
						spawnPillager(pillager.getSpawnPosition()); // респавним слайма
					}
				}
				Array<Golem> temporaryGolems = new Array<Golem>(golemDeathTimes.keySet().toArray(new Golem[0]));
				for (Golem golem : temporaryGolems) {
					golemDeathTimes.put(golem, golemDeathTimes.get(golem) + Gdx.graphics.getDeltaTime());
					if (golemDeathTimes.get(golem) >= 10) { // если прошло 5 секунд после смерти слайма
						golemDeathTimes.remove(golem); // удаляем слайма из списка мертвых слаймов
						spawnGolem(golem.getSpawnPosition()); // респавним слайма
					}
				}
				foregroundRenderer.setView(camera);
				foregroundRenderer.render();

				shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
				healthBar.renderShape(shapeRenderer, player.getHealth(), player.getMaxHealth());
				shapeRenderer.end();

				npcBatch.begin();
				alice.update(Gdx.graphics.getDeltaTime());
				alice.render(npcBatch);
				mage.update(Gdx.graphics.getDeltaTime());
				mage.render(npcBatch);
				merchant.update(Gdx.graphics.getDeltaTime());
				merchant.render(npcBatch);
				npcBatch.end();

				backgroundRenderer.setView(camera);
				backgroundRenderer.render();

				if (alice.isNearPlayer() && Gdx.input.isKeyJustPressed(Input.Keys.E)) {
					alice.interact();
				}
				if (mage.isNearPlayer() && Gdx.input.isKeyJustPressed(Input.Keys.E)) {
					mage.interact();
				}
				if (merchant.isNearPlayer() && Gdx.input.isKeyJustPressed(Input.Keys.E)) {
					merchant.interact();
				}
				if (Gdx.input.isKeyJustPressed(Input.Keys.H)) {
					player.useHealthPotion();
				}
				if (Gdx.input.isKeyJustPressed(Input.Keys.T)) {
					player.getCoins().addCoins(100);
				}
				if (Gdx.input.isKeyJustPressed(Input.Keys.U)) {
					player.takeDamage(70);
				}
				if (Gdx.input.isKeyJustPressed(Input.Keys.I)) {
					player.upgradeMaxHealth();
				}
				if (Gdx.input.isKeyJustPressed(Input.Keys.O)) {
					player.upgradeDamage();
				}
				if (Gdx.input.isKeyJustPressed(Input.Keys.P)) {
					player.addHealthPotion();
				}
				// заменяем batch на slimeBatch
				slimeBatch.begin();
				for (Damager enemy : enemies) {
					if (enemy instanceof Slime) {
						Slime slime = (Slime) enemy;
						slime.checkCollisionWithPlayer(player);
						slime.update(Gdx.graphics.getDeltaTime());
						slime.checkTargetInView(player.getPosition());
						slime.render(slimeBatch); // заменяем batch на slimeBatch
					}
				}
				slimeBatch.end();

				ghostBatch.begin();
				for (Damager enemy : enemies) {
					if (enemy instanceof Ghost) {
						Ghost ghost = (Ghost) enemy;
						ghost.checkCollisionWithPlayer(player);
						ghost.update(Gdx.graphics.getDeltaTime());
						ghost.checkTargetInView(player.getPosition());
						ghost.render(ghostBatch); // Используем ghostBatch для рендера призраков
					}
				}
				ghostBatch.end();

				pillagerBatch.begin();
				for (Damager enemy : enemies) {
					if (enemy instanceof Pillager) {
						Pillager pillager = (Pillager) enemy;
						pillager.checkCollisionWithPlayer(player);
						pillager.update(Gdx.graphics.getDeltaTime());
						pillager.checkTargetInView(player.getPosition());
						pillager.render(pillagerBatch);
					}
				}
				pillagerBatch.end();

				golemBatch.begin();
				for (Damager enemy : enemies) {
					if (enemy instanceof Golem) {
						Golem golem = (Golem) enemy;
						golem.checkCollisionWithPlayer(player);
						golem.update(Gdx.graphics.getDeltaTime());
						golem.checkTargetInView(player.getPosition());
						golem.render(golemBatch);
					}
				}
				golemBatch.end();
				checkCollisions(); // добавьте эту строку

				batch.begin();
				player.render(batch);
				batch.end();

				uiBatch.begin();
				if (alice.isActive()) {
					dialogueBox.setDialogueOpen(true);
					dialogueBox.render(uiBatch);
					alice.getDialogueBox().handleInput();
				}
				if (player.isDead()) {
					deathFont.setColor(Color.RED);
					Gdx.gl.glClearColor(0, 0, 0, 1);
					Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
					GlyphLayout layout = new GlyphLayout(deathFont, "YOU DIED!");
					deathFont.draw(uiBatch, layout, Gdx.graphics.getWidth() / 2 - layout.width / 2, Gdx.graphics.getHeight() / 2);
				}
				else {
					healthBar.renderText(uiBatch, player.getHealth(), player.getMaxHealth());
				}
				String coinText = "Coins: " + player.getCoins().getCoins();
				coinPotionFont.draw(uiBatch, coinText, 10, Gdx.graphics.getHeight() - 10);
				// Отображаем зелья

				String healthPotionText = "Health Potions: " + player.getNumHealthPotions();
				coinPotionFont.draw(uiBatch, healthPotionText, 10, Gdx.graphics.getHeight() - 40);

				String currentDamage = "Current Damage: " + player.getDamage();
				coinPotionFont.draw(uiBatch, currentDamage, 10, Gdx.graphics.getHeight() - 70);
				uiBatch.end();

				for (int i = enemies.size - 1; i >= 0; i--) {
					Damager enemy = enemies.get(i);
					if (enemy instanceof Slime && ((Slime) enemy).isDead()) {
						enemies.removeIndex(i);
						slimeDeathTimes.put((Slime) enemy, 0f); // добавляем время смерти слайма в список
					}
					else if (enemy instanceof Ghost && ((Ghost) enemy).isDead()) {
						enemies.removeIndex(i);
						ghostDeathTimes.put((Ghost) enemy, 0f); // добавляем время смерти слайма в список
					}
					else if (enemy instanceof Pillager && ((Pillager) enemy).isDead()) {
						enemies.removeIndex(i);
						pillagerDeathTimes.put((Pillager) enemy, 0f); // добавляем время смерти слайма в список
					}
					else if (enemy instanceof Golem && ((Golem) enemy).isDead()) {
						enemies.removeIndex(i);
						golemDeathTimes.put((Golem) enemy, 0f); // добавляем время смерти слайма в список
					}
				}
				break;
			case PAUSE:
				Gdx.gl.glClearColor(0, 0, 0, 1);
				Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
				menu.update();
				menu.render();
				// Рендеринг экрана паузы
				break;
		}
	}
	public void startGame() {
		state = GameState.PLAY;
		// Загрузка вашего уровня
		menu.setMenuItemText(0, "Resume");
		selectedMenuIndex = 0;
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

	private void spawnSlime(Vector2 spawnPosition) {
		System.out.println("Spawning slime at " + spawnPosition);
		TextureAtlas slimes = new TextureAtlas("enemies/slime.atlas");
		Slime slime = new Slime(slimes, 5, spawnPosition, collisionLayers, grid, player);
		enemies.add(slime);
		slimeDeathTimes.remove(slime); // удаляем слайма из списка мертвых слаймов
	}

	private void spawnGhost(Vector2 spawnPosition) {
		System.out.println("Spawning ghost at " + spawnPosition);
		TextureAtlas ghosts = new TextureAtlas("enemies/ghost.atlas");
		Ghost ghost = new Ghost(ghosts, 35, spawnPosition, collisionLayers, grid, player);
		enemies.add(ghost);
		slimeDeathTimes.remove(ghost); // удаляем слайма из списка мертвых слаймов
	}
	private void spawnPillager(Vector2 spawnPosition) {
		System.out.println("Spawning pillager at " + spawnPosition);
		TextureAtlas pillagers = new TextureAtlas("enemies/pillager.atlas");
		Pillager pillager = new Pillager(pillagers, 15, spawnPosition, collisionLayers, grid, player);
		enemies.add(pillager);
		pillagerDeathTimes.remove(pillager); // удаляем слайма из списка мертвых слаймов
	}
	private void spawnGolem(Vector2 spawnPosition) {
		System.out.println("Spawning golem at " + spawnPosition);
		TextureAtlas golems = new TextureAtlas("enemies/tilted_golem.atlas");
		Golem golem = new Golem(golems, 50, spawnPosition, collisionLayers, grid, player);
		enemies.add(golem);
		golemDeathTimes.remove(golem); // удаляем слайма из списка мертвых слаймов
	}
	private void checkCollisions() {
		Rectangle playerRect = player.getBoundingRectangle();
		for (Damager enemy : enemies) {
			if (enemy instanceof Slime) {
				Slime slime = (Slime) enemy;
				Rectangle slimeRect = slime.getBoundingRectangle();
				if (Intersector.overlaps(playerRect, slimeRect)) {
					// Коллизия обнаружена
					if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
						// Если клавиша space нажата, вызываем метод takeDamage для слайма
						slime.takeDamage(player.getDamage());
						player.stopMoving();
						slime.stopMoving();
					} else {
						player.takeDamage(slime.getDamage());

						// Если клавиша space не нажата, игрок не может пройти сквозь слайма
						player.stopMoving();
						slime.stopMoving();
					}
				}
			} else if (enemy instanceof Ghost) {
				Ghost ghost = (Ghost) enemy;
				Rectangle ghostRect = ghost.getBoundingRectangle();
				if (Intersector.overlaps(playerRect, ghostRect)) {
					// Коллизия обнаружена
					if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
						// Если клавиша space нажата, вызываем метод takeDamage для слайма
						ghost.takeDamage(player.getDamage());
						player.stopMoving();
						ghost.stopMoving();
					} else {
						player.takeDamage(ghost.getDamage());
						// Если клавиша space не нажата, игрок не может пройти сквозь слайма
						player.stopMoving();
						ghost.stopMoving();
					}
				}
			} else if (enemy instanceof Pillager) {
				Pillager pillager = (Pillager) enemy;
				Rectangle pillagerRect = pillager.getBoundingRectangle();
				if (Intersector.overlaps(playerRect, pillagerRect)) {
					// Коллизия обнаружена
					if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
						// Если клавиша space нажата, вызываем метод takeDamage для слайма
						pillager.takeDamage(player.getDamage());
						player.stopMoving();
						pillager.stopMoving();
					} else {
						player.takeDamage(pillager.getDamage());
						// Если клавиша space не нажата, игрок не может пройти сквозь слайма
						player.stopMoving();
						pillager.stopMoving();
					}
				}
			} else if (enemy instanceof Golem) {
				Golem golem = (Golem) enemy;
				Rectangle golemRect = golem.getBoundingRectangle();
				if (Intersector.overlaps(playerRect, golemRect)) {
					// Коллизия обнаружена
					if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
						// Если клавиша space нажата, вызываем метод takeDamage для слайма
						golem.takeDamage(player.getDamage());
						player.stopMoving();
						golem.stopMoving();
					} else {
						player.takeDamage(golem.getDamage());
						// Если клавиша space не нажата, игрок не может пройти сквозь слайма
						player.stopMoving();
						golem.stopMoving();
					}
				}
			}
		}
	}

	@Override
	public void dispose() {
		batch.dispose();
		map.dispose();
		foregroundRenderer.dispose();
		backgroundRenderer.dispose();
		playerAtlas.dispose();
		healthFont.dispose();
		unitsHealthFont.dispose();
		dialogFont.dispose();
		deathFont.dispose(); // Добавляем dispose для deathFont
		slimeBatch.dispose(); // освобождаем ресурсы нового SpriteBatch
		ghostBatch.dispose(); // Освобождаем ресурсы нового SpriteBatch
		pillagerBatch.dispose(); // Освобождаем ресурсы нового SpriteBatch
		golemBatch.dispose(); // Освобождаем ресурсы нового SpriteBatch
		npcBatch.dispose();
		coinPotionFont.dispose();
	}
}