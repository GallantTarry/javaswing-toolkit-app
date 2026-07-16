# -*- coding: utf-8 -*-
import os
import pandas as pd
import tkinter as tk
from tkinter import filedialog, messagebox

def select_folder():
    """调起系统弹窗选择文件夹"""
    folder_path = filedialog.askdirectory(title="请选择存放待合并Excel的文件夹")
    if folder_path:
        entry_folder.delete(0, tk.END)
        entry_folder.insert(0, folder_path)

def start_merge():
    """核心合并逻辑"""
    folder_path = entry_folder.get().strip()

    # 1. 校验输入
    if not folder_path:
        messagebox.showwarning("操作提示", "请先选择存放待合并Excel的文件夹！")
        return

    # 2. 锁定按钮防误触
    btn_start.config(text="正在拼命合并中...", state=tk.DISABLED)
    root.update()

    try:
        all_data = []
        file_count = 0
        error_files = []

        # 3. 遍历读取机制
        for file in os.listdir(folder_path):
            if (file.endswith('.xlsx') or file.endswith('.xls')) and not file.startswith('~$'):
                # 防无限套娃：跳过工具自己生成的汇总表
                if "汇总表_合并结果" in file:
                    continue

                file_path = os.path.join(folder_path, file)
                try:
                    # 修复点 1: sheet_name=None 读取所有 Sheet
                    # 修复点 2: dtype=str 强制所有数据作为文本读取，防止银行卡号、单号等长数字变科学计数法导致丢失
                    sheet_dict = pd.read_excel(file_path, sheet_name=None, dtype=str)

                    # 遍历这个文件里的所有 Sheet
                    for sheet_name, df in sheet_dict.items():
                        # 修复点 3: 剔除完全空白的无效行，防止空数据撑大表格
                        df = df.dropna(how='all')

                        if not df.empty:
                            # 插入来源标识，加上 Sheet 名更精准
                            df.insert(0, '数据来源_Sheet名', sheet_name)
                            df.insert(0, '数据来源_源文件名', file)
                            all_data.append(df)

                    file_count += 1
                except Exception as e:
                    # 记录读取失败的文件
                    print(f"读取异常: {file}, 错误: {e}") # 可以在控制台看一眼具体报错
                    error_files.append(file)

        # 4. 数据合并与输出
        if all_data:
            # 将所有收集到的 dataframe 合并成一个
            merged_df = pd.concat(all_data, ignore_index=True)

            # 定义输出路径
            output_file_path = os.path.join(folder_path, "汇总表_合并结果.xlsx")

            # 导出为 Excel 文件
            merged_df.to_excel(output_file_path, index=False)

            # 构建成功提示语
            msg = f"✅ 合并圆满完成！\n\n共读取了 {file_count} 个文件，总计合并 {len(merged_df)} 行数据。\n汇总表已安全保存至该文件夹下。"
            if error_files:
                msg += f"\n\n⚠️ 另有 {len(error_files)} 个文件读取失败被跳过(可能被密码加密或损坏)。"
            messagebox.showinfo("合并成功", msg)
        else:
            messagebox.showwarning("合并失败", "任务结束：没有提取到任何有效数据。\n请检查该文件夹下是否有合法的 Excel 文件，或者表格是否全为空。")

    except Exception as e:
        messagebox.showerror("运行异常", f"保存失败！\n提示：请检查目标文件夹是否有写入权限，或者“汇总表_合并结果.xlsx”是否正被 Excel 软件占用。\n\n报错详情: {e}")

    finally:
        # 5. 恢复按钮状态
        btn_start.config(text="开始一键合并", state=tk.NORMAL)


# ================= 构建可视化操作面板 =================
root = tk.Tk()
root.title("Excel 多表一键合并工具")
root.geometry("480x160")
root.resizable(False, False)

# 确保窗口弹在最前，防遮挡
root.attributes('-topmost', True)

padx, pady = 15, 15

# 第一行：文件夹选择
tk.Label(root, text="待合并文件夹:").grid(row=0, column=0, padx=padx, pady=pady, sticky="e")
entry_folder = tk.Entry(root, width=35)
entry_folder.grid(row=0, column=1, pady=pady)
tk.Button(root, text="浏览...", command=select_folder).grid(row=0, column=2, padx=10, pady=pady)

# 第二行：操作按钮
btn_start = tk.Button(root, text="开始一键合并", bg="#4CAF50", fg="black", font=("Arial", 11, "bold"), width=18, command=start_merge)
btn_start.grid(row=1, column=0, columnspan=3, pady=10)

if __name__ == "__main__":
    root.mainloop()