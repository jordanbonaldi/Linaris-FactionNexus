package com.massivecraft.factions.cmd;

import com.massivecraft.factions.P;

public class CmdMoney extends FCommand {
    public CmdMoneyBalance cmdMoneyBalance = new CmdMoneyBalance();
    public CmdMoneyDeposit cmdMoneyDeposit = new CmdMoneyDeposit();
    public CmdMoneyWithdraw cmdMoneyWithdraw = new CmdMoneyWithdraw();
    public CmdMoneyTransferFf cmdMoneyTransferFf = new CmdMoneyTransferFf();
    public CmdMoneyTransferFp cmdMoneyTransferFp = new CmdMoneyTransferFp();
    public CmdMoneyTransferPf cmdMoneyTransferPf = new CmdMoneyTransferPf();

    public CmdMoney() {
        super();
        aliases.add("money");

        //this.requiredArgs.add("");
        //this.optionalArgs.put("","")

        isMoneyCommand = true;

        senderMustBePlayer = false;
        senderMustBeMember = false;
        senderMustBeModerator = false;
        senderMustBeAdmin = false;

        this.setHelpShort("faction money commands");
        helpLong.add(p.txt.parseTags("<i>The faction money commands."));

        this.addSubCommand(cmdMoneyBalance);
        this.addSubCommand(cmdMoneyDeposit);
        this.addSubCommand(cmdMoneyWithdraw);
        this.addSubCommand(cmdMoneyTransferFf);
        this.addSubCommand(cmdMoneyTransferFp);
        this.addSubCommand(cmdMoneyTransferPf);
    }

    @Override
    public void perform() {
        commandChain.add(this);
        P.p.cmdAutoHelp.execute(sender, args, commandChain);
    }

}
