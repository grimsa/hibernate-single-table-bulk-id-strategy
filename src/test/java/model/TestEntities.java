package model;

import java.util.UUID;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;

/**
 * Nested classes model the following entity hierarchy:
 *
 * <pre>
 *       Animal
 *       /   \
 *      /     \
 *   Mammal   Reptile
 *    / \
 *   /   \
 * Human   Dog
 * </pre>
 *
 * @see http://in.relation.to/2005/07/20/multitable-bulk-operations/
 */
public class TestEntities {

    private TestEntities() {
    }

    @Entity(name = "Animal")
    @Inheritance(strategy = InheritanceType.JOINED)
    public static class Animal {

        @Id @GeneratedValue public UUID id;
    }

    @Entity(name = "Mammal")
    public static class Mammal extends Animal {
        public String mammalField;
    }

    @Entity(name = "Reptile")
    public static class Reptile extends Animal {
    }

    @Entity(name = "Human")
    public static class Human extends Mammal {
    }

    @Entity(name = "Dog")
    public static class Dog extends Mammal {
    }

}
