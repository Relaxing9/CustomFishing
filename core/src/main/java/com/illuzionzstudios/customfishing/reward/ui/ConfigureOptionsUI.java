/**
 * Copyright © 2020 Property of Illuzionz Studios, LLC
 * All rights reserved. No part of this publication may be reproduced, distributed, or
 * transmitted in any form or by any means, including photocopying, recording, or other
 * electronic or mechanical methods, without the prior written permission of the publisher,
 * except in the case of brief quotations embodied in critical reviews and certain other
 * noncommercial uses permitted by copyright law. Any licensing of this software overrides
 * this statement.
 */
package com.illuzionzstudios.customfishing.reward.ui;

import com.illuzionzstudios.customfishing.reward.FishingReward;
import com.illuzionzstudios.customfishing.reward.config.Configurable;
import com.illuzionzstudios.customfishing.settings.FishingLocale;
import com.illuzionzstudios.mist.compatibility.XMaterial;
import com.illuzionzstudios.mist.compatibility.XSound;
import com.illuzionzstudios.mist.config.locale.Message;
import com.illuzionzstudios.mist.conversation.SimpleConversation;
import com.illuzionzstudios.mist.conversation.SimplePrompt;
import com.illuzionzstudios.mist.conversation.type.SimpleDecimalPrompt;
import com.illuzionzstudios.mist.ui.UserInterface;
import com.illuzionzstudios.mist.ui.button.Button;
import com.illuzionzstudios.mist.ui.render.ItemCreator;
import com.illuzionzstudios.mist.util.ReflectionUtil;
import com.illuzionzstudios.mist.util.TextUtil;
import com.illuzionzstudios.mist.util.Valid;
import org.apache.commons.lang.WordUtils;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.Prompt;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Menu to view all options to configure for a {@link com.illuzionzstudios.customfishing.reward.FishingReward}
 * Abstracted to an item to configure those fields
 * <p>
 * A maximum of 54 options per object
 * <p>
 * TODO: May abstract to Mist Library
 *
 * @param <T> Type of item to configure for
 *            All public non-final fields will appear for configuration
 *            <p>
 *            Supported Types:
 *            {@link Integer}
 *            {@link Double}
 *            {@link Float}
 *            {@link Boolean}
 *            {@link String}
 *            {@link com.illuzionzstudios.mist.compatibility.XSound}
 *            {@link com.illuzionzstudios.mist.compatibility.XMaterial}
 *            {@link com.illuzionzstudios.mist.config.locale.Message}
 *            {@link org.bukkit.inventory.ItemStack}
 *            And {@link java.util.Collection} of aforementioned
 *            <p>
 *            Else a blank menu appears with no options to configure
 */
public class ConfigureOptionsUI<T> extends UserInterface {

    /**
     * The object to configure
     */
    public T object;

    /**
     * List of option buttons
     */
    private final List<Button> options;

    /**
     * @param object The object instance to configure
     */
    public ConfigureOptionsUI(T object) {
        this(null, object);
    }

    /**
     * @param parent Parent {@link UserInterface}
     * @param object The object instance to configure
     */
    public ConfigureOptionsUI(UserInterface parent, T object) {
        super(parent, false);
        setTitle("&8Configure Options");

        this.options = new ArrayList<>();
        this.object = object;

        // Set all options
        prepareOptions();
    }

    /**
     * Called to scan object for classes and set buttons to
     * open further configuring fields
     */
    private void prepareOptions() {
        // Make sure fresh list
        options.clear();

        // Scan each field and set button based on type
        // Type of object
        Class<?> lookup = object.getClass();

        // Now scan fields to set buttons
        for (final Field f : lookup.getDeclaredFields()) {
            // Make sure field is annotated
            if (f.getAnnotationsByType(Configurable.class).length == 0) continue;

            // Get raw field description
            // Describes what the field is
            String valueDescriptionKey = f.getAnnotation(Configurable.class).description();

            // Get the type of field for checking
            Class<?> type = f.getType();

            // The prompt to accept value
            Prompt valuePrompt = null;
            Button.ButtonListener listener = Button.ButtonListener.ofNull();

            // Message to indicate to input a value
            String enterValueMessage = FishingLocale.getMessage("rewards.config.enter-value").getMessage();

            // Open appropriate menu
            if (int.class.isAssignableFrom(type)) {
                valuePrompt = new SimpleDecimalPrompt(enterValueMessage, d -> {
                    try {
                        f.setInt(object, (int) Math.round(d));
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }

                    // We have to recreate menu with updated object
                    new ConfigureOptionsUI<>(getParent(), object).show(getViewer());
                });
            } else if (float.class.isAssignableFrom(type)) {
                valuePrompt = new SimpleDecimalPrompt(enterValueMessage, d -> {
                    try {
                        f.setFloat(object, d.floatValue());
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }

                    // We have to recreate menu with updated object
                    new ConfigureOptionsUI<>(getParent(), object).show(getViewer());
                });
            } else if (double.class.isAssignableFrom(type)) {
                valuePrompt = new SimpleDecimalPrompt(enterValueMessage, d -> {
                    try {
                        f.setDouble(object, d);
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }

                    // We have to recreate menu with updated object
                    new ConfigureOptionsUI<>(getParent(), object).show(getViewer());
                });
            } else if (boolean.class.isAssignableFrom(type)) {
                valuePrompt = new SimplePrompt() {
                    @Override
                    protected String getPrompt(ConversationContext conversationContext) {
                        return enterValueMessage;
                    }

                    @Nullable
                    @Override
                    protected Prompt acceptValidatedInput(@NotNull ConversationContext conversationContext, @NotNull String s) {
                        try {
                            f.setBoolean(object, Valid.parseBoolean(s));
                        } catch (IllegalAccessException e) {
                            e.printStackTrace();
                        }

                        // We have to recreate menu with updated object
                        new ConfigureOptionsUI<>(getParent(), object).show(getViewer());
                        return null;
                    }
                };
            } else if (String.class.isAssignableFrom(type)) {
                valuePrompt = new SimplePrompt() {
                    @Override
                    protected String getPrompt(ConversationContext conversationContext) {
                        return enterValueMessage;
                    }

                    @Nullable
                    @Override
                    protected Prompt acceptValidatedInput(@NotNull ConversationContext conversationContext, @NotNull String s) {
                        try {
                            f.set(object, s);
                        } catch (IllegalAccessException e) {
                            e.printStackTrace();
                        }

                        // We have to recreate menu with updated object
                        new ConfigureOptionsUI<>(getParent(), object).show(getViewer());
                        return null;
                    }
                };
            } else if (XSound.class.isAssignableFrom(type)) {
                valuePrompt = new SimplePrompt() {
                    @Override
                    protected String getPrompt(ConversationContext conversationContext) {
                        return enterValueMessage;
                    }

                    @Nullable
                    @Override
                    protected Prompt acceptValidatedInput(@NotNull ConversationContext conversationContext, @NotNull String s) {
                        try {
                            f.set(object, XSound.matchXSound(s).get());
                        } catch (IllegalAccessException e) {
                            e.printStackTrace();
                        }

                        // We have to recreate menu with updated object
                        new ConfigureOptionsUI<>(getParent(), object).show(getViewer());
                        return null;
                    }

                    @Override
                    protected boolean isInputValid(final ConversationContext context, final String input) {
                        return XSound.matchXSound(input).isPresent();
                    }

                    @Override
                    protected String getFailedValidationText(final ConversationContext context, final String invalidInput) {
                        return "&cPlease enter a valid sound";
                    }
                };
            } else if (XMaterial.class.isAssignableFrom(type)) {
                valuePrompt = new SimplePrompt() {
                    @Override
                    protected String getPrompt(ConversationContext conversationContext) {
                        return enterValueMessage;
                    }

                    @Nullable
                    @Override
                    protected Prompt acceptValidatedInput(@NotNull ConversationContext conversationContext, @NotNull String s) {
                        try {
                            f.set(object, XMaterial.matchXMaterial(s).get());
                        } catch (IllegalAccessException e) {
                            e.printStackTrace();
                        }

                        // We have to recreate menu with updated object
                        new ConfigureOptionsUI<>(getParent(), object).show(getViewer());
                        return null;
                    }

                    @Override
                    protected boolean isInputValid(final ConversationContext context, final String input) {
                        return XMaterial.matchXMaterial(input).isPresent();
                    }

                    @Override
                    protected String getFailedValidationText(final ConversationContext context, final String invalidInput) {
                        return "&cPlease enter a valid material";
                    }
                };
            } else if (Message.class.isAssignableFrom(type)) {
                valuePrompt = new SimplePrompt() {
                    @Override
                    protected String getPrompt(ConversationContext conversationContext) {
                        return enterValueMessage;
                    }

                    @Nullable
                    @Override
                    protected Prompt acceptValidatedInput(@NotNull ConversationContext conversationContext, @NotNull String s) {
                        try {
                            f.set(object, new Message(s));
                        } catch (IllegalAccessException e) {
                            e.printStackTrace();
                        }

                        // We have to recreate menu with updated object
                        new ConfigureOptionsUI<>(getParent(), object).show(getViewer());
                        return null;
                    }
                };
                // Collections of types
            } else if (List.class.isAssignableFrom(type)) {
                // Get the class type of the list. Messy reflection stuff
                Class<?> listType = (Class<?>) ((ParameterizedType) f.getGenericType()).getActualTypeArguments()[0];

                // For now only strings and item stacks since that's all we're dealing with
                // TODO: Add more when abstracting to library
                if (String.class.isAssignableFrom(listType)) {
                    // TODO: Open GUI to change lines
                } else if (ItemStack.class.isAssignableFrom(listType)) {
                    listener = (player, ui, clickType, event) -> {
                        new ConfigureItemsUI(this, object, f).show(getViewer());
                    };
                }
            }

            // Construct listener from prompt
            // Set listener for configuring UI based on type
            // Set final to pass to implementation
            final Prompt finalValuePrompt = valuePrompt;
            if (finalValuePrompt != null) {
                listener = (player, ui, clickType, event) -> {
                    new SimpleConversation() {
                        @Override
                        protected Prompt getFirstPrompt() {
                            return finalValuePrompt;
                        }
                    }.start(player);
                };
            }

            // Finally construct button
            Button button = Button.of(ItemCreator.builder()
                            .material(XMaterial.PAPER)
                            .name(FishingLocale.getMessage("interface.option.name")
                                    .processPlaceholder("valueName", TextUtil.convertCamelCase(f.getName())).getMessage())
                            .lore(FishingLocale.getMessage("interface.option.lore")
                                    .processPlaceholder("value", ReflectionUtil.getFieldContent(f, object))
                                    .processPlaceholder("description", WordUtils.wrap(FishingLocale.getMessage(valueDescriptionKey).getMessage(), 30)).getMessage())
                            .build(),
                    listener);
            options.add(button);
        }

        // TODO: Temp until fully abstracted to auto resize
        setSize(18);
    }

    /**
     * Map options to each slot
     */
    public ItemStack getItemAt(final int slot) {
        if (slot >= options.size())
            return ItemCreator.builder().name(" ").material(XMaterial.BLACK_STAINED_GLASS_PANE).build().makeUIItem();
        return options.get(slot) != null ? options.get(slot).getItem() : null;
    }

    /**
     * @return List of each button for each option
     */
    @Override
    protected List<Button> getButtonsToRegister() {
        return options;
    }

    /**
     * Internal class for separate UI's.
     * For instance, dragging item stacks, string lists
     */
    private final class ConfigureItemsUI extends UserInterface {

        /**
         * Items displayed in the menu
         */
        private final List<Button> items;

        /**
         * Actual list of items displayed
         */
        private final List<ItemStack> itemStacks;

        /**
         * The object to configure
         */
        public final T object;

        /**
         * Field we are setting
         * We know it's a {@link List} of {@link ItemStack}
         */
        public final Field field;

        /**
         * @param parent Parent {@link UserInterface}
         * @param object The object instance to configure
         */
        public ConfigureItemsUI(UserInterface parent, T object, Field field) {
            super(parent, false);
            setTitle("&8Configure Items");
            setSize(54);

            this.items = new ArrayList<>();
            this.object = object;
            this.field = field;

            // Assign items
            this.itemStacks = (ArrayList<ItemStack>) ReflectionUtil.getFieldContent(field, object);
            loadItems();
        }

        /**
         * Load the items from the list into the interface
         */
        private void loadItems() {
            this.itemStacks.forEach(itemStack -> {
                // Finally construct button
                Button button = Button.of(ItemCreator.builder().item(itemStack)
                                .build(),
                        (player, ui, clickType, event) -> {
                        });
                items.add(button);
            });
        }

        protected void onInterfaceClick(final Player player, final int slot, final ItemStack clicked, final InventoryClickEvent event) {
            // Allow to place items in interface
        }

        protected void onItemPlace(final Player player, final int slot, final ItemStack placed, final InventoryClickEvent event) {
            // Allow to place items in interface
        }

        @Override
        protected void onButtonClick(final Player player, final int slot, final InventoryAction action,
                                     final ClickType click, final Button button, final InventoryClickEvent event) {
            // Allow to freely drag items
            button.getListener().onClickInInterface(player, this, click, event);
        }

        /**
         * Save all item contents into reward
         */
        protected void onInterfaceClose(final Player player, final Inventory inventory) {
            List<ItemStack> contents = new ArrayList<>();

            for (int i = 0; i < inventory.getSize(); i++) {
                // Don't save return button
                if (i == getReturnButtonPosition()) continue;

                ItemStack stack = inventory.getItem(i);

                if (stack == null || stack.getType() == Material.AIR) continue;

                contents.add(stack);
            }

            try {
                field.set(object, contents);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }

        /**
         * Map options to each slot
         */
        public ItemStack getItemAt(final int slot) {
            if (slot >= items.size())
                return null;
            return items.get(slot) != null ? items.get(slot).getItem() : null;
        }

        /**
         * @return List of each button for each item
         */
        @Override
        protected List<Button> getButtonsToRegister() {
            return items;
        }

    }

}
