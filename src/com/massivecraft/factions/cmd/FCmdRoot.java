package com.massivecraft.factions.cmd;

import java.util.Collections;

import com.massivecraft.factions.Conf;

public class FCmdRoot extends FCommand {
    public CmdAdmin cmdAdmin = new CmdAdmin();
    public CmdAutoClaim cmdAutoClaim = new CmdAutoClaim();
    public CmdBoom cmdBoom = new CmdBoom();
    public CmdBypass cmdBypass = new CmdBypass();
    public CmdChat cmdChat = new CmdChat();
    public CmdChatSpy cmdChatSpy = new CmdChatSpy();
    public CmdClaim cmdClaim = new CmdClaim();
    public CmdConfig cmdConfig = new CmdConfig();
    public CmdCreate cmdCreate = new CmdCreate();
    public CmdDeinvite cmdDeinvite = new CmdDeinvite();
    public CmdDescription cmdDescription = new CmdDescription();
    public CmdDisband cmdDisband = new CmdDisband();
    public CmdHelp cmdHelp = new CmdHelp();
    public CmdHome cmdHome = new CmdHome();
    public CmdInvite cmdInvite = new CmdInvite();
    public CmdJoin cmdJoin = new CmdJoin();
    public CmdKick cmdKick = new CmdKick();
    public CmdLeave cmdLeave = new CmdLeave();
    public CmdList cmdList = new CmdList();
    public CmdLock cmdLock = new CmdLock();
    public CmdMap cmdMap = new CmdMap();
    public CmdMod cmdMod = new CmdMod();
    public CmdMoney cmdMoney = new CmdMoney();
    public CmdOpen cmdOpen = new CmdOpen();
    public CmdOwner cmdOwner = new CmdOwner();
    public CmdOwnerList cmdOwnerList = new CmdOwnerList();
    public CmdPeaceful cmdPeaceful = new CmdPeaceful();
    public CmdPermanent cmdPermanent = new CmdPermanent();
    public CmdPermanentPower cmdPermanentPower = new CmdPermanentPower();
    public CmdPowerBoost cmdPowerBoost = new CmdPowerBoost();
    public CmdPower cmdPower = new CmdPower();
    public CmdRelationAlly cmdRelationAlly = new CmdRelationAlly();
    public CmdRelationEnemy cmdRelationEnemy = new CmdRelationEnemy();
    public CmdRelationNeutral cmdRelationNeutral = new CmdRelationNeutral();
    public CmdReload cmdReload = new CmdReload();
    public CmdSafeunclaimall cmdSafeunclaimall = new CmdSafeunclaimall();
    public CmdSaveAll cmdSaveAll = new CmdSaveAll();
    public CmdSethome cmdSethome = new CmdSethome();
    public CmdShow cmdShow = new CmdShow();
    public CmdTag cmdTag = new CmdTag();
    public CmdTitle cmdTitle = new CmdTitle();
    public CmdUnclaim cmdUnclaim = new CmdUnclaim();
    public CmdUnclaimall cmdUnclaimall = new CmdUnclaimall();
    public CmdVersion cmdVersion = new CmdVersion();
    public CmdWarunclaimall cmdWarunclaimall = new CmdWarunclaimall();

    public FCmdRoot() {
        super();
        aliases.addAll(Conf.baseCommandAliases);
        aliases.removeAll(Collections.singletonList(null)); // remove any nulls from extra commas
        allowNoSlashAccess = Conf.allowNoSlashCommand;

        //this.requiredArgs.add("");
        //this.optionalArgs.put("","")

        senderMustBePlayer = false;
        senderMustBeMember = false;
        senderMustBeModerator = false;
        senderMustBeAdmin = false;

        disableOnLock = false;

        this.setHelpShort("The faction base command");
        helpLong.add(p.txt.parseTags("<i>This command contains all faction stuff."));

        //this.subCommands.add(p.cmdHelp);

        this.addSubCommand(cmdAdmin);
        this.addSubCommand(cmdAutoClaim);
        this.addSubCommand(cmdBoom);
        this.addSubCommand(cmdBypass);
        this.addSubCommand(cmdChat);
        this.addSubCommand(cmdChatSpy);
        this.addSubCommand(cmdClaim);
        this.addSubCommand(cmdConfig);
        this.addSubCommand(cmdCreate);
        this.addSubCommand(cmdDeinvite);
        this.addSubCommand(cmdDescription);
        this.addSubCommand(cmdDisband);
        this.addSubCommand(cmdHelp);
        this.addSubCommand(cmdHome);
        this.addSubCommand(cmdInvite);
        this.addSubCommand(cmdJoin);
        this.addSubCommand(cmdKick);
        this.addSubCommand(cmdLeave);
        this.addSubCommand(cmdList);
        this.addSubCommand(cmdLock);
        this.addSubCommand(cmdMap);
        this.addSubCommand(cmdMod);
        this.addSubCommand(cmdMoney);
        this.addSubCommand(cmdOpen);
        this.addSubCommand(cmdOwner);
        this.addSubCommand(cmdOwnerList);
        this.addSubCommand(cmdPeaceful);
        this.addSubCommand(cmdPermanent);
        this.addSubCommand(cmdPermanentPower);
        this.addSubCommand(cmdPower);
        this.addSubCommand(cmdPowerBoost);
        this.addSubCommand(cmdRelationAlly);
        this.addSubCommand(cmdRelationEnemy);
        this.addSubCommand(cmdRelationNeutral);
        this.addSubCommand(cmdReload);
        this.addSubCommand(cmdSafeunclaimall);
        this.addSubCommand(cmdSaveAll);
        this.addSubCommand(cmdSethome);
        this.addSubCommand(cmdShow);
        this.addSubCommand(cmdTag);
        this.addSubCommand(cmdTitle);
        this.addSubCommand(cmdUnclaim);
        this.addSubCommand(cmdUnclaimall);
        this.addSubCommand(cmdVersion);
        this.addSubCommand(cmdWarunclaimall);
    }

    @Override
    public void perform() {
        commandChain.add(this);
        cmdHelp.execute(sender, args, commandChain);
    }

}
