# -*- coding: utf-8 -*-
import os
import openpyxl
import pandas as pd
import tkinter as tk
from tkinter import filedialog, messagebox

def select_folder():
    """调起系统弹窗选择文件夹"""
    folder_path = filedialog.askdirectory(title="请选择存放待提取Excel的文件夹")
    if folder_path:
        entry_folder.delete(0, tk.END)
        entry_folder.insert(0, folder_path)

def start_extraction():
    """核心提取逻辑"""
    folder_path = entry_folder.get().strip()
    cells_input = entry_cells.get().strip()

    # 1. 校验输入框是否为空
    if not folder_path:
        messagebox.showwarning("操作提示", "请先选择存放Excel文件的文件夹！")
        return
    if not cells_input:
        messagebox.showwarning("操作提示", "请填写需要提取的单元格坐标！\n例如: B2, D5, E10")
        return

    # 2. 锁定按钮，防止重复点击
    btn_start.config(text="正在拼命提取中...", state=tk.DISABLED)
    root.update()

    try:
        # 清洗用户输入：转大写、去空格、按逗号拆分
        target_cells = [cell.strip().upper() for cell in cells_input.split(',') if cell.strip()]
        result_data = []
        success_count = 0
        error_files = []

        # 3. 遍历与读取机制
        for file in os.listdir(folder_path):
            if file.endswith('.xlsx') and not file.startswith('~$'):
                if "专项提取汇总" in file:
                    continue
                    
                file_path = os.path.join(folder_path, file)
                
                try:
                    wb = openpyxl.load_workbook(file_path, data_only=True, read_only=True)
                    ws = wb.active 
                    
                    row_data = {"数据来源_源文件名": file}
                    
                    for cell in target_cells:
                        try:
                            row_data[cell] = ws[cell].value
                        except Exception:
                            row_data[cell] = "[坐标无效]"
                    
                    result_data.append(row_data)
                    wb.close()
                    success_count += 1
                except Exception as e:
                    error_files.append(file)

        # 4. 数据汇总与输出
        if success_count > 0:
            df = pd.DataFrame(result_data)
            cols = ['数据来源_源文件名'] + target_cells
            df = df[cols]
            
            output_path = os.path.join(folder_path, "专项提取汇总台账.xlsx")
            df.to_excel(output_path, index=False)
            
            msg = f"✅ 提取圆满完成！\n\n共从 {success_count} 个文件中成功提取数据。\n汇总台账已在目标文件夹中生成。"
            if error_files:
                msg += f"\n\n⚠️ 有 {len(error_files)} 个文件读取失败(可能被加密或损坏)。"
            messagebox.showinfo("提取成功", msg)
        else:
            messagebox.showwarning("提取失败", "任务结束：没有提取到任何有效数据。\n请检查该文件夹下是否有格式正确的 .xlsx 文件。")

    except Exception as e:
        messagebox.showerror("运行异常", f"处理过程中发生严重错误，请检查目标表是否被打开。\n\n报错详情: {e}")
    
    finally:
        # 5. 恢复按钮状态
        btn_start.config(text="开始批量提取", state=tk.NORMAL)


# ================= 构建可视化操作面板 =================
root = tk.Tk()
root.title("Excel 特定单元格批量提取工具")
root.geometry("520x220")
root.resizable(False, False)

padx, pady = 15, 12

# 第一行：文件夹选择
tk.Label(root, text="待提取的文件夹:").grid(row=0, column=0, padx=padx, pady=pady, sticky="e")
entry_folder = tk.Entry(root, width=38)
entry_folder.grid(row=0, column=1, pady=pady)
tk.Button(root, text="浏览...", command=select_folder).grid(row=0, column=2, padx=10, pady=pady)

# 第二行：单元格输入
tk.Label(root, text="目标单元格坐标:").grid(row=1, column=0, padx=padx, pady=pady, sticky="e")
entry_cells = tk.Entry(root, width=38)
entry_cells.grid(row=1, column=1, pady=pady)
entry_cells.insert(0, "B2, D5, E10") # 提供默认占位示例

# 第三行：操作按钮
btn_start = tk.Button(root, text="开始批量提取", bg="#2196F3", fg="black", font=("Arial", 11, "bold"), width=18, command=start_extraction)
btn_start.grid(row=2, column=0, columnspan=3, pady=20)

if __name__ == "__main__":
    root.mainloop()