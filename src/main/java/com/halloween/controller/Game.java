package com.halloween.controller;

import static com.halloween.view.SoundEffects.playSound;

import com.halloween.model.House;
import com.halloween.model.Neighborhood;
import com.halloween.model.Player;
import com.halloween.model.State;
import com.halloween.view.PlayMusic;
import com.halloween.view.View;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;


public class Game {
  private State state;
  private View display = new View();
  private Player player = new Player();
  private Neighborhood neighborhood = new Neighborhood();
  private StoreGame storeGame = new StoreGame();
  private PlayMusic musicPlayer = new PlayMusic();

  public Game() throws IOException {
    player.setPosition("your house");
  }

  public Game(State state, Player player, Neighborhood neighborhood) {
    this.state = state;
    this.player = player;
    this.neighborhood = neighborhood;
  }

  public void greetPlayer() throws IOException {
    BufferedReader buffer = new BufferedReader(new InputStreamReader(System.in));
    System.out.println(display.getNpcResponse("ask_name"));
    player.setName(buffer.readLine().trim());
    if (player.getName().equals("quit")) quitGame();
    System.out.printf(display.getNpcResponse("welcome"), player.getName());
  }
  public void showStatus() {
    House currentPosition =  neighborhood.getNeighborhood().get(player.getPosition());
    String playerItems = player.getItems().isEmpty() ? "nothing" : player.getItems().toString();
    String houseItems = currentPosition.getHouseItems().isEmpty() ? "a whole lot of nothing" : currentPosition.getHouseItems().toString();

    System.out.printf(display.getNpcResponse("house_item"), currentPosition.getHouseName(), houseItems);
    System.out.printf(display.getNpcResponse("item_in_inventory"), player.getName(), player.getPosition(), playerItems);
    showValidMoves();
  }

  public void showMenu(){
    System.out.println(display.getMenu());;
  }
  public void showTitle() {
    System.out.println(display.getTitle());
  }
  public void showBackstory() {
    System.out.println(display.getBackstory());
  }
  public void showInstructions() {
    System.out.println(display.getInstructions());
  }
  public void showHelp() {
    System.out.println(display.getHelp());
  }
  public void showInventory() {
    System.out.printf(display.getNpcResponse("show_inventory"), player.getItems());
  }
  public void showMap(){
    System.out.println(display.getMap());
  }
  public void showValidMoves() {
    House currentPosition =  neighborhood.getNeighborhood().get(player.getPosition());
    String north = currentPosition.getNorth() != null ? "\nnorth: " + currentPosition.getNorth() : "";
    String east = currentPosition.getEast() != null ? "\neast: " + currentPosition.getEast(): "";
    String south = currentPosition.getSouth() != null ? "\nsouth: " + currentPosition.getSouth() : "";
    String west = currentPosition.getWest() != null ? "\nwest: " + currentPosition.getWest() : "";
    System.out.println(north + east + south + west);
  }

  public void movePlayer(String direction) {
    House currentPosition =  neighborhood.getNeighborhood().get(player.getPosition());
    String playersMove = neighborhood.isValidDirection(direction, currentPosition);
    // set the previous house knocked to false before moving
    currentPosition.setKnocked(false);
    if (playersMove.isEmpty()){
      System.out.printf(display.getNpcResponse("invalid_direction"), direction);
      showValidMoves();
    } else {
      player.setPosition(playersMove);
      System.out.printf( display.getNpcResponse("players_move"), player.getName(), direction, player.getPosition());
      playSound("/footsteps.wav");
    }
  }

  public void getItem() {
    House house =  neighborhood.getNeighborhood().get(player.getPosition());
    if (house.isKnocked() && house.getHouseItems().size() > 0) {
        String temp = house.getHouseItems().get(0);
        player.addItem(temp);
        house.removeItem();
        System.out.printf(display.getNpcResponse("get_items"), temp);
    } else if (house.isKnocked()){
        System.out.println(display.getNpcResponse("no_item_error"));
    } else {
      System.out.println(display.getNpcResponse("knock_door_first"));
      System.out.println(display.getNpcResponse("knock_door"));
    }
    house.setKnocked(false);
  }

  public void knockOnDoor() {
    House house =  neighborhood.getNeighborhood().get(player.getPosition());
    house.setKnocked(true);

    String knock = "/door-knock.wav";
    playSound(knock);

    ArrayList<String> playerItems = player.getItems();
    // If we knock on karen's house or the saw house we need to have check for specific items in our inventory
    // If we do not have the items, then we lose the game
      // If we knock on karen's door
    if (house.getHouseName().equals("karen's house")) {
      knockOnKarenHouse(playerItems);
      // if knock on the saw house
    } else if (house.getHouseName().equals("saw house")) {
      knockOnSawHouse(playerItems);
      // for all other houses (besides karen's house and saw house) we do the following
    } else if (house.getHouseItems().isEmpty()) {
      display.noItem(player.getPosition());
    } else {
      display.greet(player.getPosition());
    }
  }

  private void knockOnSawHouse(ArrayList<String> playerItems) {
    // check for "thing" in not in our items then we lose the game
    if (!playerItems.contains("thing")){
      display.noItem(player.getPosition());
      setState(State.LOSE);
    } // otherwise, thing will free us from the trap, and be removed from the inventory
    else {
      // System.out.println("Suddenly, thing jumps from your candy bag, and frees you! RUN WHILE YOU CAN!");
      display.greet(player.getPosition());
      player.removeItem("thing");
    }
  }

  private void knockOnKarenHouse(ArrayList<String> playerItems) {
    // if we have a badge, potion, or ruby, then do nothing
    if (playerItems.contains("badge") || playerItems.contains("potion") || playerItems.contains("ruby")) {
      System.out.println(display.getNpcResponse("karen_calling_cops"));
    }
    // if we don't have a badge, potion, or ruby we lose the game
    else {
      display.greet(player.getPosition());
      System.out.println(display.getNpcResponse("player_arrested"));
      playSound("/evil-shreik.wav");
      setState(State.LOSE);
    }
  }

  public void quitGame() {
    System.out.println(display.getNpcResponse("exit_game"));
    System.exit(0);
  }
  public void saveGame() {
    storeGame.saveGame(state, player, neighborhood);
  }
  public void loadGame() {
    storeGame.loadGame();
  }
  public State getState() {
    return state;
  }

  public void setState(State state) {
    this.state = state;
  }

  public void useItem(String item) {
    // get the house the player is currently at
    House house = neighborhood.getNeighborhood().get(player.getPosition());
    boolean successfullyUsedItem = player.removeItem(item);
    // if the house is knocked then try to use the item
    if (house.isKnocked()) {
      showInventory();
      String response = successfullyUsedItem ? "remove_item" : "warning_remove_item";
      System.out.printf(display.getNpcResponse(response), item);
      if (!successfullyUsedItem) return;
    } else {
      System.out.println(display.getNpcResponse("knock_to_use_item"));
      return;
    }
    // if we use the badge at karen's house then we win the game
    if (house.getHouseName().equals("karen's house")) {
      karenUseItem(item);
    } else if (house.getHouseName().equals("dracula's mansion") && item.equals("tooth")) {
      System.out.println(display.getNpcResponse("draculas_tooth"));
      // added dracula's ruby to our inventory
      // NOTE: dracula's tooth is a hidden item, so we don't store it in the house
      player.addItem("ruby");
    } else if (house.getHouseName().equals("witch's den")) {
       witchUseItem(item, house);
     }
  }

  private void witchUseItem(String item, House house) {
    if (item.equals("cat-hair") || item.equals("beer") || item.equals("dentures")) {
      System.out.printf(display.getNpcResponse("give_witch_ingredient"), item);
      playSound("/bubbles.wav");
      house.addItem(item);
    } else {
      System.out.printf(display.getNpcResponse("incorrect_witch_ingredient"), item);
    }
    ArrayList<String> witchHouseItems = house.getHouseItems();
    if (witchHouseItems.contains("cat-hair") && witchHouseItems.contains("beer") && witchHouseItems.contains("dentures")) {
      System.out.println(display.getNpcResponse("complete_witch_potion"));
      // NOTE: potion is a hidden item, so we don't store it in the house
      player.addItem("potion");
      playSound("/witch.wav");
    }
  }

  public void karenUseItem(String item) {
    if (item.equals("badge")) {
      System.out.println(display.getNpcResponse("karen_defeated_badge"));
      playSound("/girl_scream.wav");
    } else if (item.equals("potion")) {
      System.out.println(display.getNpcResponse("karen_defeated_potion"));
    } else if (item.equals("ruby")) {
      System.out.println(display.getNpcResponse("karen_defeated_ruby"));
    } else {
      return;
    }
    setState(State.WIN);
  }
  public void startMusic() {
    String musicName = "/darkess.wav";
    musicPlayer.play(musicName);
  }

  public void stopMusic() {
    musicPlayer.stop();
  }

  public void increaseVolume() {
    musicPlayer.increaseVolume();
  }

  public void decreaseVolume() {
    musicPlayer.decreaseVolume();
  }

}
