package at.htl.quiz.model;

import at.htl.quiz.db.Repo;
import at.htl.quiz.ui.MainMenuController;
import at.htl.quiz.util.CommandLineUtil;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class Game {
    public enum currentstage {
        MAINMENU,
        GAME,
        EDITQUESTIONS
    }

    private MainMenuController controller;
    private Player player;
    private Repo db;
    Stage mainStage;
    currentstage currentstage = null;

    private List<AskedQuestion> questionsAnswered = new LinkedList<>();
    private int score = 0;


    public Game(Player player) {
        this();
        this.player = player;
    }

    public Game(Player player, Repo db) {
        this.player = player;
        this.db = db;
    }

    public Game() {
        this.player = null;
    }

    public Game(Repo db) {
        this.db = db;
    }

    public Player getPlayer() {
        return player;
    }

    public List<AskedQuestion> getQuestionsAnswered() {
        return questionsAnswered;
    }

    public int getScore() {
        return score;
    }

    /**
     * Starts the UI for the game
     *
     * @param stage
     * @throws IOException fxml file not found
     */
    public void startUI(Stage stage) throws IOException {
        this.mainStage = stage;
        MainMenuController controller = new MainMenuController();
        controller.show(stage);
        this.currentstage = this.currentstage.MAINMENU;
    }

    public void addAnswerOption(AskedQuestion questions) {
        this.questionsAnswered.add(questions);
        if(questions.getAnswer().isCorrect()) {
            incrementScore(questions.getAnswer().getPoints());
        }
    }

    public void incrementScore(int number) {
        this.score += number;
    }

    public Question getQuestion()  {
        try {
            List<Question> list = this.db.getQuestions(1);
            if(list == null) {
                return null;
            }
            return list.get(0);
        } catch (SQLException e) {
            return null;
        }
    }

    public void startCommandLineGame() {
        int points = 0;
        int questionCount = 0;
        List<AskedQuestion> asked = new LinkedList<>();

        boolean isRunning = true;
        while (isRunning) {
            try {
                //get random question:
                var question = this.db.getQuestions(1).get(0);

                //print question
                System.out.println(String.join("", "-".repeat(question.getText().length() + 10)));
                System.out.println("Question: " + question.getText());

                //output questions
                AtomicInteger questionCounter = new AtomicInteger('a');
                question.getAnswers().forEach(answer -> {
                    char letter = (char) questionCounter.getAndIncrement();
                    System.out.println(letter + ") " + answer.getText());
                });

                //let the user answer
                Answer answer = null;
                while (true) {
                    try {
                        System.out.print("\nAnswer: ");
                        String inputString = CommandLineUtil.getLine();
                        int input = inputString.charAt(0) - 'a';
                        answer = question.getAnswers().get(input);
                    } catch (Exception e) {
                        System.out.println("Invalid Input, try again");
                        e.printStackTrace();
                        continue;
                    }
                    break;
                }

                //check answer:
                assert answer != null;
                questionCount++;
                asked.add(new AskedQuestion(question, answer));
                if (answer.isCorrect()) {
                    points++;
                    System.out.println("CORRECT!");
                } else {
                    //print out right answers if user is wrong:
                    System.out.println("Sorry, your answer is wrong!");
                    System.out.println("Correct answers: ");
                    question.getCorrectAnswers().forEach(answer1 -> {
                        char l = (char) ('a' +  question.getAnswers().indexOf(answer1));
                        System.out.println("- " + l + ") ");
                    });
                }

                //ask user if he wants another question:
                System.out.print("Do you want another question? (Y/n)");
                while(true) {
                    String input = CommandLineUtil.getLine();
                    if(input.equalsIgnoreCase("n")) {
                        isRunning = false;
                    } else if (!(input.trim().equals("") || input.equalsIgnoreCase("y"))) {
                        System.out.print("Invalid Input, try again: ");
                        continue;
                    }
                    break;
                }
            } catch (SQLException e) {
                e.printStackTrace();
                System.out.println("Something went wrong with the Database connection!");
            }
        }
        //end of game:

        //print statistics of the game
        System.out.println("=".repeat(40));
        System.out.println("Statistics: ".toUpperCase(Locale.ROOT));
        System.out.println("Questions answered: " + questionCount);
        System.out.println("Points: " + points);

        //save status to DB:
        try {
            db.saveGame(this);
        } catch (SQLException e) {
            System.out.println("Error while saving game in DB!");
        }
    }

    /**
     * Ends the game and saves everything to the database;
     * @return true if saving successfull, false otherwise
     */
    public boolean end() {
        //TODO: save to DB
        try {
            this.db.saveGame(this);
            this.db.close();
        } catch (SQLException e) {
            System.err.println("Could not close database connection: " + e.getMessage());
            return false;
        }
        return true;
    }
}
