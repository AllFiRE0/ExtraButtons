package me.homas343.extrabuttons;

import org.bukkit.entity.Player;
import me.clip.placeholderapi.PlaceholderAPI;

public class ConditionChecker {
    
    public static boolean checkConditions(Player player, java.util.List<String> conditions) {
        if (conditions == null || conditions.isEmpty()) return true;
        
        for (String condition : conditions) {
            if (!checkCondition(player, condition)) {
                return false;
            }
        }
        return true;
    }
    
    private static boolean checkCondition(Player player, String condition) {
        String[] operators = {">=", "<=", ">", "<", "!=", "=", "<-", "!<-", "|-", "!|-", "-|", "!-|"};
        String operator = null;
        String[] parts = null;
        
        for (String op : operators) {
            if (condition.contains(op)) {
                operator = op;
                parts = condition.split(op, 2);
                break;
            }
        }
        
        if (operator == null || parts == null || parts.length != 2) {
            return true;
        }
        
        String left = parts[0].trim();
        String right = parts[1].trim();
        
        String leftValue = PlaceholderAPI.setPlaceholders(player, left);
        String rightValue = PlaceholderAPI.setPlaceholders(player, right);
        
        return evaluateCondition(leftValue, rightValue, operator);
    }
    
    private static boolean evaluateCondition(String left, String right, String operator) {
        switch (operator) {
            case "=":
                return left.equals(right);
            case "!=":
                return !left.equals(right);
            case "<-":
                return left.contains(right);
            case "!<-":
                return !left.contains(right);
            case "|-":
                return left.startsWith(right);
            case "!|-":
                return !left.startsWith(right);
            case "-|":
                return left.endsWith(right);
            case "!-|":
                return !left.endsWith(right);
            case ">":
            case ">=":
            case "<":
            case "<=":
                return compareNumbers(left, right, operator);
            default:
                return true;
        }
    }
    
    private static boolean compareNumbers(String left, String right, String operator) {
        try {
            double leftNum = Double.parseDouble(left);
            double rightNum = Double.parseDouble(right);
            
            switch (operator) {
                case ">":
                    return leftNum > rightNum;
                case ">=":
                    return leftNum >= rightNum;
                case "<":
                    return leftNum < rightNum;
                case "<=":
                    return leftNum <= rightNum;
                default:
                    return true;
            }
        } catch (NumberFormatException e) {
            return true;
        }
    }
}