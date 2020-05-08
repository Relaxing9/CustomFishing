package com.illuzionzstudios.customfishing.command.sub;

import com.illuzionzstudios.command.ReturnType;
import com.illuzionzstudios.command.type.AbstractCommand;
import com.illuzionzstudios.customfishing.CustomFishing;
import com.illuzionzstudios.customfishing.controller.RewardsController;
import com.illuzionzstudios.customfishing.struct.Permission;

/**
 * Copyright © 2020 Property of Illuzionz Studios, LLC
 * All rights reserved. No part of this publication may be reproduced, distributed, or
 * transmitted in any form or by any means, including photocopying, recording, or other
 * electronic or mechanical methods, without the prior written permission of the publisher,
 * except in the case of brief quotations embodied in critical reviews and certain other
 * noncommercial uses permitted by copyright law. Any licensing of this software overrides
 * this statement.
 */

public class ReloadCommand extends AbstractCommand {

    private final CustomFishing plugin;

    public ReloadCommand(CustomFishing plugin) {
        super("reload", "rl");

        this.plugin = plugin;

        this.requiredPermission = Permission.RELOAD;
    }

    @Override
    public ReturnType onCommand(String s, String[] strings) {
        plugin.reloadConfig();
        plugin.getLocale().getMessage("general.reload").sendPrefixedMessage(commandSender);

        return ReturnType.SUCCESS;
    }

    @Override
    public boolean isConsoleAllowed() {
        return true;
    }
}
